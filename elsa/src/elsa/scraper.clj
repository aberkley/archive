(ns schneider.scraper
  (:require [clj-http.client :as client]
            [clj-webdriver.taxi :as taxi]
            [clj-webdriver.core :as driver]
            [net.cgrand.enlive-html :as html]))

(defn html-resource-
  [url]
  (-> url
      client/get
      :body
      html/html-snippet))

(def quickrank-stem "http://lt.morningstar.com/ova4sc327n/fundquickrank")

(def snapshot-stem "http://lt.morningstar.com/ova4sc327n/snapshot")

(def url-default "/default.aspx")

(defn select-categories
  [driver fund-categories]
  (for [c fund-categories]
    (do
     (taxi/select-option driver "select.msSearchSelectCategory" {:text c})
     (taxi/select-option driver "select[name='ctl00$ContentPlaceHolder1$aFundQuickrankControl$ddlPageSize'" {:text "500 per page"})
     (taxi/click driver "input[title='Search']")
     (taxi/page-source driver))))

(defn get-html
  [fund-categories]
  (let [driver (-> {:browser :chrome }
                   taxi/new-driver
                   (taxi/to (str quickrank-stem url-default)))
        htmls- (doall (select-categories driver fund-categories))]
    (taxi/quit driver)
    htmls-))

(defn select-table-data
  [html-]
  (-> html-
      html/html-snippet
      (html/select [:table.gridView])
      (html/select [:tr.gridItem])))

(defn select-grid-fund-name-attrs
  [tr]
  (-> tr
      (html/select [:td.gridFundName :a])
      first
      :attrs))

(defn get-fund-snapshot-html
  [attrs]
  (->> attrs
       :href
       (str quickrank-stem "/")
       java.net.URL.
       html/html-resource))

(defn select-content
  [html- selector]
  (-> html-
      (html/select selector)
      ((comp first :content first))))

(defn tag-content
  [tag]
  (-> tag
;;      first
      :content
      first))

(def isin-selector [:div#content
                    :div#keyStatsDiv
                    [:div.snapshot_24_percent_overview_header_div (html/has [(html/re-pred #"ISIN")])]
                    :div.value_div])

(defn select-isin
  [html-]
  (select-content html- isin-selector))

(def category-selector [:div#content
                        :div#keyStatsDiv
                        [:div.snapshot_24_percent_overview_header_div (html/has [(html/re-pred (re-pattern "Morningstar.*"))])]
                        :div.value_div])

(defn select-category
  [html-]
  (select-content html- category-selector))

(defn select-fund-fees-href
  [html-]
  (-> html-
      (html/select [:div#snapshotTabNewDiv
                    [:a.msDeck (html/has [(html/re-pred #"Fees")])]])
      first
      :attrs
      :href))

(defn get-fund-fees-html
  [href]
  (-> href
      ((partial str snapshot-stem "/"))
      java.net.URL.
      (html/html-resource)))

(def initial-fee-and-symbol-selector [:div#overviewCustomFundFacts
                                      [:tr (html/has [(html/re-pred #"You Pay")])]
                                      :td.value
                                      :span])

(def management-fee-selector [:div#managementFeesDiv
                              :div#managementFeesAnnualChargesDiv
                              [:tr (html/has [(html/re-pred (re-pattern "Management \\(Max\\)"))])]
                              :td.value
                              :span])

(def minimum-investment-selector [:div#managementPurchaseInformationDiv
                                  :div#managementPurchaseInformationMinInvestDiv
                                  [:tr (html/has [(html/re-pred #"Initial")])]
                                  :td.value])

(def valuation-point-selector [:div#overviewCustomFundFacts
                               [:tr (html/has [(html/re-pred #"Valuation Point")])]
                               :td.value
                               :span])

(defn select-symbol
  [html-]
  (-> html-
      (html/select initial-fee-and-symbol-selector)
      second
      tag-content))

(defn select-initial-fee
  [html-]
  (-> html-
      (html/select initial-fee-and-symbol-selector)
      first
      tag-content))

(defn select-management-fee
  [html-]
  (select-content html- management-fee-selector))

(defn select-minimum-investment
  [html-]
  (select-content html- minimum-investment-selector))

(defn select-valuation-point
  [html-]
  (-> html-
      (html/select valuation-point-selector)
      second
      tag-content))

(defn select-fund-fees-data
  [html-]
  (-> {}
      (assoc :valuation-point (select-valuation-point html-))
      (assoc :minimum-investment (select-minimum-investment html-))
      (assoc :management-fee (select-management-fee html-))
      (assoc :initial-fee (select-initial-fee html-))
      (assoc :symbol (select-symbol html-))))

(defn scrape
  [categories]
  (let [funds (->> categories
                   get-html
                   (map select-table-data)
                   (map #(map select-grid-fund-name-attrs %))
                   flatten)
        snapshots (->> funds
                       (map get-fund-snapshot-html)
                       (map (juxt select-isin select-fund-fees-href select-category))
                       (map (partial zipmap [:isin :fund-fees-href :category])))
        fees (->> snapshots
                  (map :fund-fees-href)
                  (map get-fund-fees-html)
                  (map select-fund-fees-data))]
    (->> (map merge funds snapshots fees)
         (map #(dissoc % :href))
         (map #(dissoc % :fund-fees-href))
         (map #(dissoc % :target))
         doall)))

(comment
  (scrape ["Japan Large-Cap Equity"])
  )

(def categories ["Japan Small/Mid-Cap Equity"
                 "Japan Large-Cap Equity"
                 "US Large-Cap Value Equity"
                 "US Flex-Cap Equity"
                 "US Large-Cap Blend Equity"
                 "US Large-Cap Growth Equity"])
