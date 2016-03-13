(ns merkledag.browser.queries
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [re-frame.core :refer [register-sub]]))


(register-sub :view-state
  (fn [db _]
    (reaction
      (let [showing (:view/show @db)]
        {:view showing
         :state (get-in @db [:view/state showing])}))))


(register-sub :app-config
  (fn [db _]
    (reaction {:server-url (:server-url @db)
               :ui-counter (:ui-counter @db 0)})))


(register-sub :nodes
  (fn [db _]
    (reaction (:nodes @db))))


(register-sub :node-info
  (fn [db _ [id]]
    (reaction (get-in @db [:nodes id]))))
