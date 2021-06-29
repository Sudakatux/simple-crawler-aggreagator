(ns middleman.core
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [chime.core :as chime])
  (:gen-class))

(def weatherCloudApiURL "https://app.weathercloud.net/device/info/")
(def weatherAppRawDataUrl "http://localhost:8082/api/c/rawdata/")

(def expected-date-format )

(defn build-url [stationId]
(apply str [weatherCloudApiURL stationId]))


(defn request-weathercloud [stationId]
  "Creates a common request"
  (let [{:keys [body]} @(http/get (build-url stationId)
                                  {
                                   :headers {"X-Requested-With" "XMLHttpRequest" }
                                  })]
     body))

(defn to_wind_speed_count [metersPerSecond]
  (let [km_hour (* metersPerSecond 3.6)]
    (int (* km_hour 3600))))

;(defn one-hour-sequence-from-now []
;  (let [now (atom (java.time.Instant/now))]
;    (repeatedly
;      (fn []
;        (swap! now #(.plus % 1 java.time.temporal.ChronoUnit/MINUTES))))
                                        ;    ))
(defn one-hour-sequence-from-now []
  (chime/periodic-seq (java.time.Instant/now) (java.time.Duration/ofHours 1)))

(defn todays-date []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm") (java.util.Date.)))


(defn parse-weathercloud-to-ours [{:keys [device values]} messageNumber]
  "converts weather cloud data to raw data for stationId"
  (hash-map
    :date (todays-date), ;temperature
    :api_key "01125481799", ;humidiy
    :earthHumidity (get values :hum), ;presure
    :rain (get values :rain), ;rain
    :wind_speed_count "3600", ; wind speed
    :wind_speed_sum (str (to_wind_speed_count (Double/parseDouble (get values :wspdavg)))), ;wind gust
    :wind_vane (get values :wdiravg), ; wind gust
    :pressure, (get values :bar); voltage
    :temperature (get values :temp),
    :humidity (get values :hum),
    :earthTemperature (get values :temp) ; Earth Humidiy
    :wind_gust "4"
    :battery "4059"
    :messageNumber (str messageNumber)))

(defn to-hashmap [data]
  (json/read-str data :key-fn keyword))

(defn pass-info-to-app [wetherAppInfo]
"Hits wethercloud looking for info on stationId, then hits wetherApp with info"
  @(http/request
    {
     :url weatherAppRawDataUrl
     :method :post             ; :post :put :head or other
     :headers { "Content-Type" "application/json"}
     :user-agent "Mozilla string"
     :body (json/write-str wetherAppInfo)
     :insecure? true ; Need to contact a server with an untrusted SSL cert?
     :follow-redirects false
     }))

(defn make-request [stationId messageNumber] 
  "returns wetherApp request() payload"
  (-> stationId
      request-weathercloud
      to-hashmap
     (parse-weathercloud-to-ours messageNumber)
      pass-info-to-app))


(defn call-station-every [stationId]
  (let [messageNumber (atom 0)]
    (chime/chime-at (one-hour-sequence-from-now)
                    (fn [_]
                      (println "Running")
                      (println (make-request stationId (swap! messageNumber inc)))))))

(defn -main
  [stationId]
  (println (str "Running every hour for stationID=" stationId))
  (call-station-every stationId))

(comment
  ;(make-request )

                                       ; (get @(create-request "0576509888") :body)
;; => nil
;  (:status @(http/get (build-url "0576509888") {
;:headers {"X-Requested-With" "XMLHttpRequest" }
;                                                } ))
;; => 200
;; => 405
(to_wind_speed_count 3.3)
;; => 42768.0
;(create-request "0576509888")
;(Double/parseDouble (get  (parse-weathercloud-to-ours (json/read-str (create-request "0576509888") :key-fn keyword)) :wspdavg))


                                        ;(let  [ {:keys [values]} (json/read-str (create-request "0576509888") :key-fn keyword) ]
;values
 ; )
;; => {:dew "1.7", :temp "4.5", :solarrad "0.0", :wspdavg "2.4", :bar "1029.2", :rainrate "0.0", :uvi "0.0", :hum "82", :wdiravg "281", :rain "0.0"}
 ;; => "{\"device\":{\"account\":0,\"status\":\"2\",\"city\":\"Federal\",\"image\":null,\"favorite\":false,\"social\":false,\"altitude\":\"68.0\",\"update\":383},\"values\":{\"temp\":\"7.1\",\"hum\":\"71\",\"dew\":\"2.2\",\"wspdavg\":\"2.8\",\"wdiravg\":\"269\",\"bar\":\"1026.9\",\"rain\":\"0.0\",\"rainrate\":\"0.0\",\"solarrad\":\"0.0\",\"uvi\":\"0.0\"}}"
 ;; => nil
;; => nil
(take 4 (one-hour-sequence-from-now))
(call-station-every "0576509888")
 (todays-date ))






