(ns starter.browser)

;; start is called by init and after code reloading finishes
(defn ^:dev/after-load start [] (js/console.log "start"))
(defn ^:dev/after-load init [] (start))

(def canvas (js/document.querySelector "canvas"))
;; set canvas width and height


(defn update-canvas-size
  []
  (let [canvas (js/document.querySelector "canvas")]
    (js/console.log "resizing")
    (set! (.-width canvas) js/window.innerWidth)
    (set! (.-height canvas) js/window.innerHeight)))
(js/window.addEventListener "resize" update-canvas-size)
(update-canvas-size)

(def ctx (.getContext canvas "2d"))
(set! (.-fillStyle ctx) "#faf")



;;ctx.lineWidth = 10;
;; assign object properties

(defn draw-circle
  [x y]
  (set! (.-fillStyle ctx) "#f0f")
  (.arc ctx x y 10 0 (* Math/PI 2) true)
  (.fill ctx))

(def state (atom [{:pos {:x 0, :y 0}, :vel {:x 1, :y 0}}]))

(defn myiter
  []
  (swap! state (fn []
                 (map (fn [p]
                        (let [vel (:vel p) vel-x (:x vel) vel-y (:y vel)] p))
                   (deref state)))))


(defn render
  []
  (doall (map (fn [p]
                (js/console.log (str) p)
                (draw-circle (:x (:pos p)) (:y (:pos p))))
           (deref state))))



(defn myloop
  []
  (js/console.log "looping")
  (render)
  (myiter)
  (js/setTimeout myloop 100))

(myloop)

;; this is called before any code is reloaded
(defn ^:dev/before-load stop [] (js/console.log "stop"))
