(ns middleman.core
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [chime.core :as chime])
  (:gen-class))

(def weatherCloudApiURL "https://app.weathercloud.net/device/info/")
(def weatherAppRawDataUrl "http://localhost:8082/api/c/rawdata/")


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

(defn one-hour-sequence-from-now []
  (chime/periodic-seq (java.time.Instant/now) (java.time.Duration/ofHours 1)))

(defn todays-date []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm") (java.util.Date.)))


(defn parse-weathercloud-to-ours [{:keys [device values]} messageNumber]
  "converts weather cloud data to raw data for stationId"
  (hash-map
    :date (todays-date),
    :api_key "01125481799",
    :earthHumidity (get values :hum),
    :rain (get values :rain),
    :wind_speed_count "3600",
    :wind_speed_sum (str (to_wind_speed_count (Double/parseDouble (get values :wspdavg)))),
    :wind_vane (get values :wdiravg),
    :pressure, (get values :bar),
    :temperature (get values :temp),
    :humidity (get values :hum),
    :earthTemperature (get values :temp),
    :wind_gust "4",
    :battery "4059",
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
  "Given a weathercloud id make a request and pass it on to server parsed"
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

