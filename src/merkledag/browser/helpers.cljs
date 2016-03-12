(ns merkledag.browser.helpers
  (:require
    [alphabase.bytes :as bytes]
    [alphabase.hex :as hex]
    [cljs.pprint :refer [pprint]]
    [clojure.string :as str]
    [multihash.core :as multihash])
  (:import
    [goog.string StringBuffer]))


(defn edn-block
  "Renders an EDN value for display. Returns a preformatted block."
  [value]
  [:pre (let [sb (StringBuffer.)
              out (StringBufferWriter. sb)]
          (pprint value out)
          (str sb))])


(defn hexedit-block
  "Returns a preformatted block reminiscent of hex-editor views, with 16 bytes
  shown per line in both hexadecimal and ascii (where printable).

  Lines will be formatted like this:

  ```
  00 01 02 03 04 05 06 07  08 09 0a 0b 0c 0d 0e 0f   ........ ........
  00 01 02 03                                        ....
  ```
  "
  [data]
  (let [hex-section #(str (str/join " " (map hex/byte->hex %))
                          (when (< (count %) 8)
                            (str/join (repeat (- 8 (count %)) "   "))))
        byte->char #(if (<= 32 % 126)
                      (.fromCharCode js/String %)
                      ".")
        ascii-section #(str/join (map byte->char %))]
    (->> (bytes/byte-seq data)
         (partition-all 16)
         (map (fn hexedit-line
                [line-data]
                (let [left (take 8 line-data)
                      right (drop 8 line-data)]
                  (str (hex-section left)
                       "  "
                       (hex-section right)
                       "   "
                       (ascii-section left)
                       " "
                       (ascii-section right)))))
         (str/join "\n")
         (vector :pre))))
