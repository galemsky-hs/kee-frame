(ns ^:no-doc kee-frame.state)

(def controllers (atom {}))

(def controllers-enabled? (atom false))

(def last-route-to-apply-to-controllers (atom nil))

(def router (atom nil))

(def navigator (atom nil))

(def app-db-spec (atom nil))

(def debug? (atom false))

(def debug-config (atom nil))

(def fsm-interceptors (atom {}))

(def breakpoints-initialized? (atom false))

;; Test utility
(defn reset-state! []
  (reset! controllers {})
  (reset! fsm-interceptors {})
  (reset! router nil)
  (reset! last-route-to-apply-to-controllers nil)
  (reset! controllers-enabled? false)
  (reset! navigator nil))
