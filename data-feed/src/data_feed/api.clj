(ns data-feed.api)

(defn get-update-requests [as-of config]
  "returns a list of data still outstanding based on the time and the configuration")


(comment
  [{:type 'historical-data
    :securities ["TPX Index" "GXZ4 Index" "GBP.USD"]
    :frequency  ["Daily"]
    :start-time ["2014-01-01"]
    :end-time 'now
    }
   {:type 'reference-data
    :securities []
    :fields ["FUND_NET_ASSET_VAL"]}]
  )

(defn process-data! [])

(comment
  [
   {:type 'historical-data
    :data [{"Security" "TPX Index"} ]}
   {:type 'reference-data
    :data [{:product/fund-net-asset-val 2013.45}]}]
  )
