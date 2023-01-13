(ns ^:no-doc kee-frame.interop
  (:require [kee-frame.api :as api]
            [reagent.dom :as reagent-dom]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [breaking-point.core :as bp]
            [accountant.core :as accountant]
            [re-frame.loggers :as rf.log]))

(defrecord AccountantNavigator []
  api/Navigator
  (dispatch-current! [_]
    #_(accountant/dispatch-current!))
  (navigate! [_ url]
    #_(accountant/navigate! url)))

(defn make-navigator
  [opts]
  #_(accountant/configure-navigation! opts)
  (->AccountantNavigator))

(defn render-root [root-component]
  (when root-component
    (if-let [app-element (.getElementById js/document "app")]

      (reagent-dom/render root-component
                          app-element)
      (throw (ex-info "Could not find element with id 'app' to mount app into" {:component root-component})))))

(defn breakpoints-or-defaults [breakpoints]
  (or breakpoints
      {:debounce-ms 166
       :breakpoints [:mobile
                     768
                     :tablet
                     992
                     :small-monitor
                     1200
                     :large-monitor]}))

(defn set-breakpoint-subs [breakpoints]
  (bp/register-subs (:breakpoints (breakpoints-or-defaults breakpoints))))

(defn set-breakpoints [breakpoints]
  (rf/dispatch-sync [::bp/set-breakpoints (breakpoints-or-defaults breakpoints)]))

(defn set-timeout [f ms]
  (js/setTimeout f ms))

(defn clear-timeout [t]
  (js/clearTimeout t))
