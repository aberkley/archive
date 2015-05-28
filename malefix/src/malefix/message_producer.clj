(ns malefix.message-producer)

(defprotocol MessageProducer
  (send-message [message-producer message topic]))
