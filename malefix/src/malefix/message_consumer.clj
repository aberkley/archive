(ns malefix.message-consumer)

(defprotocol MessageConsumer
  (read-message [message-consumer topic]))
