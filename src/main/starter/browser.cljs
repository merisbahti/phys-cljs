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
  [x y color]
  (.beginPath ctx)
  (set! (.-fillStyle ctx) color)
  (.arc ctx x y 10 0 (* Math/PI 2) true)
  (.fill ctx))

(def state
  (atom (map (fn [xs]
               (-> xs
                   (update-in [:vel :x] (fn [x] (+ 10 x)))
                   (update-in [:vel :y] (fn [x] (+ 10 x)))))
          [{:color "#f0f", :pos {:x 0, :y 150}, :vel {:x 1, :y 1}}
           {:color "#f00", :pos {:x 150, :y 0}, :vel {:x 0.5, :y 0.1}}
           {:color "#00f", :pos {:x 0, :y 0}, :vel {:x -5, :y -8}}
           {:color "#0F0", :pos {:x 150, :y 150}, :vel {:x 0.5, :y 0.25}}])))

(defn random-color
  "gives a random color between 000 and fff"
  []
  (-> (js/Math.random)
      (* 0xFFFFFF)
      (js/Math.ceil)
      (.toString 16)
      (.padStart 6 "0")
      (#(str "#" %))))


(js/document.addEventListener
  "click"
  (fn [x]
    (swap! state (fn [state]
                   (conj state
                         {:color (random-color),
                          :pos {:x x.pageX, :y x.pageY},
                          :vel {:x (* 10 (- 0.5 (js/Math.random))),
                                :y (* 10 (- 0.5 (js/Math.random)))}})))))

(defn force-polarity
  [pos max-pos vel]
  (cond (> pos max-pos) (* -1 (abs vel))
        (> 0 pos) (abs vel)
        true vel))


(defn myiter
  []
  (swap! state (fn [state]
                 (map (fn [p]
                        (-> p
                            (update-in [:vel :y]
                                       #(force-polarity (get-in p [:pos :y])
                                                        (.-height canvas)
                                                        (get-in p [:vel :y])))
                            (update-in [:vel :x]
                                       #(force-polarity (get-in p [:pos :x])
                                                        (.-width canvas)
                                                        (get-in p [:vel :x])))
                            (update-in [:pos :y] #(+ % (get-in p [:vel :y])))
                            (update-in [:pos :x] #(+ % (get-in p [:vel :x])))))
                   state))))


(defn render
  []
  (doall (map (fn [p] (draw-circle (:x (:pos p)) (:y (:pos p)) (:color p)))
           (deref state))))




(defn myloop
  []
  (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
  (render)
  (myiter)
  (js/requestAnimationFrame myloop))

(myloop)

;; this is called before any code is reloaded
(defn ^:dev/before-load stop [] (js/console.log "stop"))
