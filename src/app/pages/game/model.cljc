(ns app.pages.game.model
  (:require [re-frame.core :as rf]
            [zframes.storage :as storage]
            [zframes.pages :as pages]
            [clojure.string :as str]))

(def index-page ::index-page)


(def aidbox
  {"insulin"    {:name    "Insulin"
                 :img     "./img/insulin.png"
                 :price   2
                 :effects {:diarrhea      -1
                           :sugar        3
                           :bacteria     1}}

   "amoxicillin" {:name    "Amoxicillin"
                  :img     "./img/amoxicillin.png"
                  :price   5
                  :effects {:temperature  1
                            :diarrhea      -1
                            :bacteria     2}}})

(rf/reg-event-fx
 index-page
 [(rf/inject-cofx ::storage/get [:player])]
 (fn [{storage :storage  db :db} [pid phase params]]
   {:db (-> db
            (merge storage)
            (assoc :aidbox aidbox))
    :json/fetch {:uri "/Patient"
                 :params {:general-practitioner (get-in storage [:player :id])}
                 :success {:event ::save-patients}
                 :req-id ::pts}}))


(defn patients-map [resp]
  (->> resp :data :entry (map :resource)
       (reduce (fn [acc pt] (assoc acc (:id pt) pt)) {})
       (into (sorted-map))))

(def drag-zone-cfg {:drop-dispatch [:my-drop-dispatch] :drop-marker :my-drop-marker})


(rf/reg-event-db
 ::save-obs
 (fn [db [_ resp]]
   (->> resp
        :data
        :entry
        (map :resource)
        (group-by #(get-in % [:subject :id]))
        (assoc db :observations))))


(rf/reg-event-fx
 ::save-patients
 (fn [{db :db} [_ resp]]
   (let [pts (patients-map resp)
         init-droppable (map  (fn [[k v]] [:dnd/initialize-drop-zone (keyword k) drag-zone-cfg]) pts)]
     {:dispatch-n  init-droppable
      :db (assoc db :patients pts)
      :json/fetch {:uri "/Observation"
                   :params {:subject (str/join "," (keys pts))}
                   :success {:event ::save-obs}}})))

(rf/reg-sub ::aidbox (fn [db _] (:aidbox db)))
(rf/reg-sub ::patients (fn [db _] (:patients db)))
(rf/reg-sub ::observations (fn [db _] (:observations db)))


(rf/reg-sub
 index-page
 :<- [::patients]
 :<- [::observations]
 :<- [::aidbox]
 (fn [[pts obs aids] _]
   {:pts pts
    :obs obs
    :aidbox aids}))

(defn apply-stats [pt effect]
  (merge-with (fn [a b] (max 0 (min 5 (+ a b)))) pt effect))

(rf/reg-event-fx
 ::apply-drug
 (fn [{db :db} [_ pt drug]]
   (if (< (:balance pt) (:price drug))
     {:db db}
     (let [obs     (get-in db [:observations (:id pt)])
           stats   (get-in db [:observations (:id pt)])
           stats   (group-by #(get-in % [:code :coding 0 :code]) stats)
           stats   (reduce-kv (fn [acc k v]
                                (assoc acc (keyword k) (get-in v [0 :value :Quantity :value])))
                           {} stats)

           result-stats  (apply-stats stats (:effects drug))

           patient (update pt :balance - (:price drug))
           new-obs (reduce-kv
                        (fn [acc k v]
                          (conj acc
                                (-> obs
                                    (->> (filter #(= (name k) (get-in % [:code :coding 0 :code]))))
                                    first
                                    (assoc-in [:value :Quantity :value] v))))
                        []
                        result-stats)]

       {:db (-> db
                (assoc-in [:patients (:id pt)] patient)
                (assoc-in [:observations (:id pt)] new-obs))

        :json/fetch {:uri (str "/Patient/" (:id pt))
                     :method :put
                     :body patient}}))))
(rand-int 3)
(first {:foo "bar" :tar "mar"})


(defn mk-damage [_]
  {:sugar        (* -1 (rand-int 2))
   :temperature  (* -1 (rand-int 2))
   :pressure     (* -1 (rand-int 2))
   :bacteria     (* -1 (rand-int 2))
   :diarrhea     (* -1 (rand-int 2))})

(defn do-damage [pt obs damage]
  (let [stats   (group-by #(get-in % [:code :coding 0 :code]) obs)
        stats   (reduce-kv (fn [acc k v] (assoc acc (keyword k) (get-in v [0 :value :Quantity :value]))) {} stats)
        result-stats  (apply-stats stats damage)]
    (reduce-kv
     (fn [acc k v]
       (conj acc
             (-> obs
                 (->> (filter #(= (name k) (get-in % [:code :coding 0 :code]))))
                 first
                 (assoc-in [:value :Quantity :value] v))))
     []
     result-stats)))

(rf/reg-event-fx
 ::next-step
 (fn [{db :db} [_]]
   (let [result (reduce-kv
                 (fn [acc k pt]
                   (assoc acc k (do-damage pt (get-in db [:observations (:id pt)]) (mk-damage {}))))
                 {}
                 (get-in db [:patients]))]
     {:db (-> db (assoc :observations result))})))
