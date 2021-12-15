(ns app.pages.index.model
  (:require [re-frame.core :as rf]
            [zframes.storage :as storage]
            [zframes.routing :as routing]
            [zframes.pages :as pages]))

(def index-page ::index-page)

(def pt-names ["Arnbo" "Lecona" "Arbornora" "Kullorsa"
               "Ilugaars" "Kuumtoq" "Dulao" "Uruazoc"
               "Jiusave" "Somerham" "Lonwick" "Harpstone"
               "Onostone" "Bricona" "Dorrial" "Ilulisna"
               "Qassiars" "Siorapa" "Guadora" "Buenapant"
               "Coamora" "Yarcester" "Preshurst" "Buxbron"])

(defn patient-name []
  (rand-nth pt-names)
  )


(rf/reg-event-fx
 index-page
 (fn [{db :db} [pid phase params]]))

(rf/reg-event-fx
 ::start-game
 (fn [{db :db} [evid]]
   {:json/fetch {:uri "/Practitioner"
                 :method :post
                 :body {:name [{:given ["marat"]}]}
                 :success {:event ::prepare-patients}
                 :req-id evid}}))

(defn mk-patient-batch-req [{id :id :as practitioner}]
  {:request {:method "POST" :url "/Patient"}
   :resource {:name [{:given [(patient-name)]}]
              :balance 20
              :health  10
              :generalPractitioner [{:id id :resourceType "Practitioner"}]}})

(defn stat-builder [patient stat]
  {:request {:method "post" :url "/Observation"}
   :resource {:subject {:id (:id patient) :resourceType "Patient"}
              :status "final"
              :code {:coding [{:code   stat
                               :system "urn:observation"}]}
              :value {:Quantity {:value (- 5 (rand-int 3))}}}})

(defn mk-patient-stats-request
  [patient]
  [(stat-builder patient "temperature")
   (stat-builder patient "sugar")
   (stat-builder patient "pressure")
   (stat-builder patient "bacteria")
   (stat-builder patient "diarrhea")])

(rf/reg-event-fx
 ::prepare-patients
 (fn [{db :db} [evid {practitioner :data}]]
   {::storage/set {:player practitioner}
    :json/fetch {:uri "/"
                 :method :post
                 :body {:resourceType "Bundle"
                        :entry [(mk-patient-batch-req practitioner)
                                (mk-patient-batch-req practitioner)
                                (mk-patient-batch-req practitioner)]}
                 :success {:event ::save-init-data}}}))

(rf/reg-event-fx
 ::save-init-data
 (fn [{_db :db} [_evid {resp :data}]]
   (let [pts (->> resp
                  :entry
                  (map :resource))]
     {:json/fetch {:uri "/"
                   :method :post
                   :body {:resourceType "Bundle"
                          :entry (mapcat mk-patient-stats-request pts)}
                   :success {:event ::run-game}}})))

(rf/reg-event-fx
 ::run-game
 (fn [{db :db} [evid _]]
   {::routing/redirect {:ev :app.pages.game.model/index-page}}))

(rf/reg-sub
 index-page
 (fn [db _]
   {}))
