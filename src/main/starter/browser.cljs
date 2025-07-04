(ns starter.browser
  (:require [cljs.math :as math]))

;; start is called by init and after code reloading finishes

(def canvas (js/document.querySelector "canvas"))

;; set canvas width and height

(defn update-canvas-size
  []
  (let [canvas (js/document.querySelector "canvas")]
    (set! (.-width canvas) js/window.innerWidth)
    (set! (.-height canvas) js/window.innerHeight)))

(def ctx (.getContext canvas "2d"))

(defn draw-circle
  [x y color]
  (.beginPath ctx)
  (set! (.-fillStyle ctx) color)
  (.arc ctx x y 5 0 (* Math/PI 2) true)
  (.fill ctx)
  (.closePath ctx))

(defn draw-points-equispaced
  [count distance {originX :x, originY :y}]
  (map (fn [n]
         {:x (+ originX (* distance (math/sin (* (/ n count) 2 Math/PI)))),
          :y (+ originY (* distance (math/cos (* (/ n count) 2 Math/PI))))})
    (range count)))
(comment
  (draw-points-equispaced 10
    100
    {:x (/ js/window.innerWidth 2),
     :y (/ js/window.innerHeight 2)}))

(defn random-color
  "gives a random color between 000 and fff"
  []
  (-> (math/random)
    (* (/ (* 0xFFFFFF) 4))
    (+ (* 2 (/ (* 0xFFFFFF) 4)))
    (math/ceil)
    (.toString 16)
    (.padStart 6 "0")
    (#(str "#" %))))

(def state
  (atom (map (fn [point] {:color (random-color), :pos point, :vel {:x 0, :y 0}})
          (draw-points-equispaced 50
            100
            {:x (/ js/window.innerWidth 2),
             :y (/ js/window.innerHeight 2)}))))

(def mouse-pos-state (atom {:x 0, :y 0}))
(def mouse-down-pos (atom nil))
(def max-speed 10)
(def max-distance 200)

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

(defn add-vec [{xa :x, ya :y} {xb :x, yb :y}] {:x (+ xa xb), :y (+ ya yb)})
(comment
  (add-vec {:x 1, :y 2} {:x 3, :y 4}))

(defn point-gravity
  [point points]
  (reduce add-vec (map #(gravity-vec point %) points)))

(defn update-state
  []
  (swap! state
    (fn [state]
      (map (fn [point]
             #(-> point
                (update-in [:vel]    (partial add-vec  (point-gravity point state)))
                (update-in [:pos :y] (partial + (get-in point [:vel :y])))
                (update-in [:pos :x] (partial + (get-in point [:vel :x])))))
        state))))

(defn draw-line
  [{x1 :x, y1 :y} {x2 :x, y2 :y} &
   {:keys [stroke-width stroke-style], :or {stroke-width 1, stroke-style nil}}]
  (.beginPath ctx)
  (.moveTo ctx x1 y1)
  (.lineTo ctx x2 y2)
  (set! (.-lineWidth ctx) stroke-width)
  (set! (.-strokeStyle ctx) stroke-style)
  (.stroke ctx)
  (.closePath ctx))

(defn render
  []
  (doall (map (fn [p] (draw-circle (:x (:pos p)) (:y (:pos p)) (:color p)))
           @state))
  (doall (let [{m-down-x :x, m-down-y :y} @mouse-down-pos
               {mouse-x :x, mouse-y :y} @mouse-pos-state
               actual-distance (js/Math.sqrt
                                 (+ (js/Math.pow (- m-down-x mouse-x) 2)
                                   (js/Math.pow (- m-down-y mouse-y) 2)))
               distance (min actual-distance max-distance)
               stroke-width (* 10 (/ distance max-distance))]
           (when (and m-down-x m-down-y)
             (draw-line @mouse-down-pos
               @mouse-pos-state
               {:stroke-width stroke-width, :stroke-style "#800"})))))

(defonce loop-id (atom nil))

(defn raf-loop
  [id]
  (when (= id @loop-id)
    (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
    (render)
    (update-state)
    (js/requestAnimationFrame (partial raf-loop id))))

(defn ^:dev/after-load myloop [] (raf-loop (swap! loop-id inc)))

(defn init
  []
  (update-canvas-size)
  (js/window.addEventListener "resize" update-canvas-size)
  (js/document.addEventListener "mousedown"
    (fn [_]
      (reset! mouse-down-pos @mouse-pos-state)))
  (js/document.addEventListener
    "mousemove"
    (fn [x] (swap! mouse-pos-state (fn [_] {:x x.pageX, :y x.pageY}))))
  (js/document.addEventListener
    "mouseup"
    (fn [event]
      (swap! state (fn [state]
                     (let [{downX :x, downY :y} @mouse-down-pos
                           hoverX event.pageX
                           hoverY event.pageY
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
                       (conj state
                         {:color (random-color),
                          :pos {:x downX, :y downY},
                          :vel {:x xVel, :y yVel}}))))
      (swap! mouse-down-pos (fn [_] nil))))
  (myloop))
