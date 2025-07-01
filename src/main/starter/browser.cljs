(ns starter.browser
  (:require [clojure.walk :as walk]))

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

;;ctx.lineWidth = 10;
;; assign object properties

(defn draw-circle
  [x y color]
  (.beginPath ctx)
  (set! (.-fillStyle ctx) color)
  (.arc ctx x y 5 0 (* Math/PI 2) true)
  (.fill ctx))

(def state (atom [{:color "#f0f", :pos {:x 0, :y 150}, :vel {:x 0, :y 0}}]))
(def mouse-pos-state (atom {:x 0, :y 0}))
(def mouse-down-pos (atom nil))
(def mousedownstate (atom false))

(defn random-color
  "gives a random color between 000 and fff"
  []
  (-> (js/Math.random)
      (* (/ (* 0xFFFFFF) 4))
      (+ (* 3 (/ (* 0xFFFFFF) 4)))
      (js/Math.ceil)
      (.toString 16)
      (.padStart 6 "0")
      (#(str "#" %))))
(js/document.addEventListener "mousedown"
                              (fn [x]
                                (swap! mouse-down-pos (fn [_] @mouse-pos-state))
                                (swap! mousedownstate (fn [_] true))))

(def max-speed 10)
(def max-distance 200)
(js/document.addEventListener
  "mousemove"
  (fn [x] (swap! mouse-pos-state (fn [_] {:x x.pageX, :y x.pageY}))))

(js/document.addEventListener
  "mouseup"
  (fn [x]
    (swap! state (fn [state]
                   (let [{downX :x, downY :y} @mouse-down-pos
                         hoverX x.pageX
                         hoverY x.pageY
                         xDiff (js/Math.abs (- downX hoverX))
                         yDiff (js/Math.abs (- downY hoverY))
                         distance (js/Math.sqrt (+ (js/Math.pow xDiff 2)
                                                   (js/Math.pow yDiff 2)))
                         distanceFactor (/ (js/Math.min 200 distance)
                                           max-distance)
                         xySum (+ xDiff yDiff)
                         yPart (/ (- downY hoverY) xySum)
                         xPart (/ (- downX hoverX) xySum)
                         xVel (* xPart distanceFactor max-speed)
                         yVel (* yPart distanceFactor max-speed)]
                     (js/console.log distance
                                     distanceFactor
                                     (clj->js {:color (random-color),
                                               :pos {:x downX, :y downY},
                                               :vel {:x xVel, :y yVel}}))
                     (conj state
                           {:color (random-color),
                            :pos {:x downX, :y downY},
                            :vel {:x xVel, :y yVel}}))))
    (swap! mouse-down-pos (fn [_] nil))
    (swap! mousedownstate (fn [_] false))))

(defn force-polarity
  [pos max-pos vel]
  (cond (> pos max-pos) (* -1 (abs vel))
        (> 0 pos) (abs vel)
        true vel))
(def G 0.00000081)

(defn gravity-vec
  [p1 p2]
  (let [x-diff (- (get-in p2 [:pos :x]) (get-in p1 [:pos :x]))
        y-diff (- (get-in p2 [:pos :y]) (get-in p1 [:pos :y]))
        tot-diff (abs (+ x-diff y-diff))
        x-part (/ x-diff tot-diff)
        y-part (/ y-diff tot-diff)
        squared-diff (js/Math.sqrt (+ (js/Math.pow y-diff 2)
                                      (js/Math.pow x-diff 2)))
        total-force (* G squared-diff)]
    (if (< tot-diff 0.1)
      {:x 0, :y 0}
      {:x (* x-part total-force), :y (* y-part total-force)})))
(gravity-vec {:pos {:x 10, :y 10}} {:pos {:x -10, :y -10}})

(defn point-gravity
  [point points]
  (let [stuff (map #(gravity-vec point %) points)] stuff))


(defn add-vec [{xa :x, ya :y} {xb :x, yb :y}] {:x (+ xa xb), :y (+ ya yb)})
(add-vec {:x 1, :y 2} {:x 3, :y 4})

(let [pts [{:pos {:x 10, :y 10}} {:pos {:x 9.0, :y 9.0}}
           {:pos {:x 11.0, :y 11.0}}]
      pt (nth pts 0)]
  (reduce add-vec (point-gravity pt pts)))

(defn myiter
  []
  (let [myfn (fn [state]
               (map (fn [p]
                      (-> p
                          (update :vel
                                  #(add-vec %
                                            (reduce add-vec
                                              (point-gravity p state))))
                          (update-in [:vel :y]
                                     #(force-polarity (get-in p [:pos :y])
                                                      (.-height canvas)
                                                      %))
                          (update-in [:vel :x]
                                     #(force-polarity (get-in p [:pos :x])
                                                      (.-width canvas)
                                                      %))
                          (update-in [:pos :y] #(+ % (get-in p [:vel :y])))
                          (update-in [:pos :x] #(+ % (get-in p [:vel :x])))))
                 state))]
    (swap! state myfn)))

(defn render
  []
  (doall (map (fn [p] (draw-circle (:x (:pos p)) (:y (:pos p)) (:color p)))
           @state))
  (doall
    (let [{x :x, y :y} @mouse-down-pos]
      (when (and x y) (.beginPath ctx) (.moveTo ctx x y) (.lineTo ctx x y)))))

(def looping (atom true))
(defn myloop
  []
  (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
  (render)
  (myiter)
  (when @looping (js/requestAnimationFrame myloop)))

(myloop)

;; this is called before any code is reloaded
(defn ^:dev/before-load stop [] (set! looping true))
