(ns merkledag.browser.queries
  (:require-macros
    [reagent.ratom :refer [reaction]])
  (:require
    [re-frame.core :refer [register-sub]]))


(register-sub :view-state
  (fn [db _]
    (reaction
      (let [current @db
            showing (:view/show current)]
        {:view showing
         :state (get-in current [:view/state showing])
         :counter (:view/counter current)
         :loading (:view/loading current)}))))


(register-sub :app-config
  (fn [db _]
    (reaction {:server-url (:server-url @db)})))


(register-sub :nodes
  (fn [db _]
    (reaction (:nodes @db))))


(register-sub :node-info
  (fn [db _ [id]]
    (reaction (get-in @db [:nodes id]))))


(register-sub :ref-list
  (fn [db _]
    (reaction (:refs @db))))
