(ns schneider.reconciliation
  (:require [schneider.entity :as entity]
            [clj-time.core :as t]))

(def corrections
  [[(t/date-time 2014 3 17) "PAY/REC COST OF Others"     "USD" -16681.3M]
   [(t/date-time 2014 3 17) "PAY/REC COST OF Others"     "USD"	16681.8M]
   [(t/date-time 2014 3 24) "PAY/REC COST OF Others"     "USD"	16495.6M]
   [(t/date-time 2014 3 24) "PAY/REC COST OF Others"     "USD"	-16494.4M]
   [(t/date-time 2014 3 14) "COST OF Futures"	         "USD"	91900M]
   [(t/date-time 2014 3 14) "COST OF Futures"	         "USD"	-91887.5M]
   [(t/date-time 2014 3 20) "COST OF Futures"	         "USD"	92900M]
   [(t/date-time 2014 3 20) "COST OF Futures"	         "USD"	-92887.5M]
   [(t/date-time 2014 3 14) "CLEARING FEE TRADE Futures" "USD"	0.25M]
   [(t/date-time 2014 3 14) "CLEARING FEE TRADE Futures" "USD"	0.25M]
   [(t/date-time 2014 3 20) "CLEARING FEE TRADE Futures" "USD"	0.25M]
   [(t/date-time 2014 3 20) "CLEARING FEE TRADE Futures" "USD"	0.25M]
   [(t/date-time 2014 3 13) "CLEARING FEE TRADE Others"	 "USD"	0.33M]
   [(t/date-time 2014 3 13) "CLEARING FEE TRADE Others"	 "USD"	0.33M]
   [(t/date-time 2014 3 20) "CLEARING FEE TRADE Others"	 "USD"	0.33M]
   [(t/date-time 2014 3 20) "CLEARING FEE TRADE Others"	 "USD"	0.33M]
   [(t/date-time 2014 3 14) "NFA FEE Futures"	         "USD"	0.02M]
   [(t/date-time 2014 3 14) "NFA FEE Futures"	         "USD"	0.02M]
   [(t/date-time 2014 3 20) "NFA FEE Futures"	         "USD"	0.02M]
   [(t/date-time 2014 3 20) "NFA FEE Futures"	         "USD"	0.02M]
   [(t/date-time 2014 3 14) "US CCP FEE Futures"	 "USD"	0.4M]
   [(t/date-time 2014 3 14) "US CCP FEE Futures"	 "USD"	0.4M]
   [(t/date-time 2014 3 20) "US CCP FEE Futures"	 "USD"	0.4M]
   [(t/date-time 2014 3 20) "US CCP FEE Futures"	 "USD"	0.4M]
   [(t/date-time 2014 3 14) "US TRADING FEE Futures"	 "USD"	0.75M]
   [(t/date-time 2014 3 14) "US TRADING FEE Futures"	 "USD"	0.75M]
   [(t/date-time 2014 3 20) "US TRADING FEE Futures"	 "USD"	0.75M]
   [(t/date-time 2014 3 20) "US TRADING FEE Futures"	 "USD"	0.75M]
   [(t/date-time 2014 5 1)  "CLEARING FEE TRADE Futures" "JPY" 780M]
   [(t/date-time 2014 5 1)  "CLEARING FEE TRADE Futures" "GBP" -3M]])

(comment
  (-> (entity/find-unique {:db/id 17592197154126})
      (assoc :transaction/units 0.33M)
      (assoc :transaction/value 0.33M)
      vector
      entity/update!)
  )
