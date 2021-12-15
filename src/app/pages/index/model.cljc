(ns app.pages.index.model
  (:require [re-frame.core :as rf]
            [zframes.routing :as routing]
            [zframes.pages :as pages]))

(def index-page ::index-page)

(rf/reg-event-fx
 index-page
 (fn [{db :db} [pid phase params]]))

(rf/reg-event-fx
 ::start-game
 (fn [{db :db} [evid]]
   {:json/fetch {:uri "/Practitioner"
                 :method :post
                 :body {:name [{:given ["marat"]}]}
                 :success {:event ::run-game}
                 :req-id evid}}))


(rf/reg-event-fx
 ::run-game
 (fn [{db :db} [evid {d :data}]]
   {:db (assoc db :player d)
    ::routing/redirect {:ev :app.pages.game.model/index-page}}))

(rf/reg-sub
 index-page
 (fn [db _]
   {}))
