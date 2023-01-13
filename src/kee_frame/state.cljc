(ns ^:no-doc kee-frame.state
  (:require [re-frame.interop :as interop]))

(def controllers (atom interop/empty-queue))

(def controllers-enabled? (atom false))
(def controllers-enabled-fn (atom nil))

(def router (atom nil))

(def navigator (atom nil))

(def breakpoints-initialized? (atom false))

;; Test utility
(defn reset-state! []
  (reset! controllers interop/empty-queue)
  (reset! router nil)
  (reset! controllers-enabled? false)
  (reset! navigator nil))
