(ns starter.browser)

;; start is called by init and after code reloading finishes
(defn ^:dev/after-load start [] (js/console.log "start"))
(defn ^:dev/after-load init [])

(def canvas (js/document.querySelector "canvas"))
;; set canvas width and height


(set! (.-width canvas) js/window.innerWidth)
(set! (.-height canvas) js/window.innerHeight)

(def ctx (.getContext canvas "2d"))
(set! (.-fillStyle ctx) "#faf")



;;ctx.lineWidth = 10;
;; assign object properties

(defn draw-circle
  [x y]
  (set! (.-fillStyle ctx) "#f0f")
  (.arc ctx 1 1 10 0 (* Math/PI 2) true)
  (.fill ctx))

(def state (atom [{:pos {:x 0, :y 50}, :vel {:x 1, :y 0}}]))

(defn iter
  []
  (swap! state (fn [s]
                 (map (fn [p]
                        (assoc p :pos (update-in (:pos p) [:x] + (:vel p))))
                   s))))

(defn render
  []
  (for [p (deref state)]
    (do (js/console.log p) (draw-circle (:x (:pos p)) (:y (:pos p))))))


(draw-circle 5 5)
(draw-circle 10 10)
(js/console.log ctx)
;; (defn myloop
;;   []
;;   (js/console.log "looping")
;;   (iter)
;;   (render)
;;   (js/setTimeout myloop 1000))

;; (myloop)

;; this is called before any code is reloaded
(defn ^:dev/before-load stop [] (js/console.log "stop"))
