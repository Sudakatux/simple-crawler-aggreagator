(ns middleman.core
  (:require [org.httpkit.client :as http]
            [clojure.data.json :as json])
)

(def weatherCloudApiURL "https://app.weathercloud.net/device/")
(def weatherAppRawDataUrl "http://localhost:8080/c/api/rawdata")


(defn create-request [suffix stationId]
  "Creates a common request"
  (http/request {:url (apply str [weatherCloudApiURL suffix])
    :method :post             ; :post :put :head or other
    :user-agent "Mozilla string"
    :form-params {"d" stationId} ; just like query-params, except sent in the body
    :insecure? true ; Need to contact a server with an untrusted SSL cert?
    :follow-redirects false
    })
)

(defn parse-wethercloud-to-ours [stationId hashIn]
  "converts weather cloud data to raw data for stationId"
  (hash-map 
    :field1 (get hashIn :temp), ;temperature
    :field2 (get hashIn :hum), ;humidiy
    :field3 (get hashIn :bar), ;presure
    :field4 (get hashIn :rain), ;rain
    :field5 (get hashIn :wspd), ; wind speed
    :field6 nil, ;wind gust
    :field7 (get hashIn :wdir), ; wind gust
    :field8 nil, ; voltage
    :field9 nil, ; Earth Humidiy
    :field10 nil, ; Earth Humidiy
    :api_key stationId ; Earth Humidiy
  )
)

(defn make-request [stationId] 
  "returns wetherApp request() payload"
  (let [response (get @(create-request "ajaxvalues" stationId) :body)]
    (parse-wethercloud-to-ours stationId (json/read-str response :key-fn keyword))
  )
)

; (defn passInfoToApp [wetherAppInfo]
;   "Hits wethercloud looking for info on stationId, then hits wetherApp with info"
;   (http/request {:url weatherAppRawDataUrl 
;   :method :post             ; :post :put :head or other
;   :user-agent "Mozilla string"
;   :body (json/write-str wetherAppInfo :key-fn keyword)
;   :insecure? true ; Need to contact a server with an untrusted SSL cert?
;   :follow-redirects false
;   })
; )

  
