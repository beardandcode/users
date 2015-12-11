(ns com.beardandcode.users.store.mock)

(defprotocol IMockUserStore
  (clear-users [_]))
