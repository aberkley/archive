(ns aurora.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-bootstrap.button :as b]
            [om-bootstrap.panel :as p]))

(def app-state (atom {:text "Hello world!"}))

(defn widget [data owner]
  (om/component
     (p/panel {:header (:text data)}  (b/toolbar {}
           (b/button {} (:text data))
           (b/button {:bs-style "primary"} (:text data))
           (b/button {:bs-style "success"} (:text data))
           (b/button {:bs-style "info"} (:text data))
           (b/button {:bs-style "warning"} (:text data))
           (b/button {:bs-style "danger"} (:text data))
           (b/button {:bs-style "link"} (:text data))))))

(om/root widget app-state
  {:target (. js/document (getElementById "app0"))})



(swap! app-state assoc :text "this is cool shit")
