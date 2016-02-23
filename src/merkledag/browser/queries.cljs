(ns merkledag.browser.queries
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [re-frame.core :refer [register-sub]]))


(register-sub :showing
  (fn [db _]
    (reaction (:show @db))))


(register-sub :blocks
  (fn [db _]
    (reaction (:blocks @db))))
