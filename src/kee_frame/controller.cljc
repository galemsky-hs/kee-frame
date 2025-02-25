(ns ^:no-doc kee-frame.controller
  (:require
   #?(:cljs
      [cljs.core.match :refer [match]])
   #?(:clj
      [clojure.core.match :refer [match]])
   [kee-frame.state :as state]
   [kee-frame.spec :as spec]
   [clojure.spec.alpha :as s]
   [expound.alpha :as e]
   [taoensso.timbre :as log]
   [re-frame.core :as rf]))

(defn process-params [params route db]
  (cond
    (vector? params) (get-in route params)
    (ifn? params) (params route db)))

(defn validate-and-dispatch! [dispatch]
  (when dispatch
    (log/debug "Dispatch returned from controller function " dispatch)
    (do
      (when-not (s/valid? ::spec/event-vector dispatch)
        (e/expound ::spec/event-vector dispatch)
        (throw (ex-info "Invalid dispatch value"
                        (s/explain-data ::spec/event-vector dispatch))))
      dispatch)))

(defn stop-controller [ctx {:keys [stop] :as controller}]
  (log/debug {:type       :controller-stop
              :controller controller
              :ctx        ctx})
  (cond
    (vector? stop) stop
    (ifn? stop) (validate-and-dispatch! (stop ctx))))

(defn start-controller [ctx {:keys [last-params start] :as controller}]
  (log/debug {:type       :controller-start
              :controller controller
              :ctx        ctx})
  (when start
    (cond
      (vector? start) (conj start last-params)
      (ifn? start) (validate-and-dispatch! (start ctx last-params)))))

(defn controller-actions [controllers route db]
  (reduce (fn [actions {:keys [id last-params params start stop]}]
            (let [current-params (process-params params route db)
                  controller     {:id          id
                                  :start       start
                                  :stop        stop
                                  :last-params current-params}]
              (match [last-params current-params (= last-params current-params)]
                     [_ _ true] actions
                     [nil _ false] (update actions :start conj controller)
                     [_ nil false] (update actions :stop conj controller)
                     [_ _ false] (-> actions
                                     (update :stop conj controller)
                                     (update :start conj controller)))))
          {}
          controllers))

(defn update-controllers [controllers new-controllers]
  (let [id->new-controller (->> new-controllers
                                (map (juxt :id identity))
                                (into {}))]
    (map (fn [{:keys [id] :as controller}]
           (if-let [updated-controller (id->new-controller id)]
             (assoc controller :last-params (:last-params updated-controller))
             controller))
         controllers)))

(rf/reg-event-fx ::start-controllers
  (fn [_ [_ dispatches]]
    ;; Another dispatch to make sure all controller stop commands are processed before the starts
    {:dispatch-n dispatches}))

(defn controller-effects [controllers ctx route]
  (let [{:keys [start stop]} (controller-actions controllers route (:db ctx))
        start-dispatches (map #(start-controller ctx %) start)
        stop-dispatches  (map #(stop-controller ctx %) stop)
        dispatch-n       (cond
                           (and (seq start) (seq stop)) (conj stop-dispatches
                                                              [::start-controllers start-dispatches])
                           (seq start) start-dispatches
                           (seq stop) stop-dispatches)]
    {:update-controllers (concat start stop)
     :dispatch-n         dispatch-n}))

(rf/reg-fx :update-controllers
  (fn [new-controllers]
    (swap! state/controllers update-controllers new-controllers)))

(defn enable!
  "Intended to be called externally
  (When a page loads the user role and is ready to check their permissions and run controllers)."
  [db]
  (reset! state/controllers-enabled? true)

  (let [route (:kee-frame/route db nil)]
    (rf/dispatch [:kee-frame.router/route-changed route])))
