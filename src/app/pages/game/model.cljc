(ns app.pages.game.model
  (:require [re-frame.core :as rf]
            [zframes.storage :as storage]
            [zframes.pages :as pages]))

(def index-page ::index-page)

(rf/reg-event-fx
 index-page
 [(rf/inject-cofx ::storage/get [:player])]
 (fn [{storage :storage  db :db} [pid phase params]]
   {:db (merge db storage)
    :json/fetch {:uri "/Patient"
                 :params {:general-practitioner (get-in storage [:player :id])}
                 :req-id ::pts}}))

(rf/reg-sub
 index-page
 :<- [:xhr/response ::pts]
 (fn [pts _]
   {:pts (->> pts :data :entry (map :resource))}))
