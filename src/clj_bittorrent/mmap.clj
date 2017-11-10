(ns clj-bittorrent.mmap
  (:require [clojure.java.io :as io])
  (:import (java.io RandomAccessFile Closeable File)
           (java.nio.channels FileChannel FileChannel$MapMode)
           (clojure.lang Indexed Seqable)
           (java.nio MappedByteBuffer)))

(set! *warn-on-reflection* true)

(def ^:private bytes-per-map
  "The number of bytes a single MappedByteBuffer will store"
  Integer/MAX_VALUE)

(definterface ISize
  (^long size []))

(deftype Mmap [^RandomAccessFile fis ^FileChannel fc maps]
  ISize
  (size [this] (.size fc))

  Indexed
  (nth [this i] (get maps i))
  (nth [this i not-found] (get maps i not-found))

  Seqable
  (seq [this] (seq maps))

  Closeable
  (close
    [this]
    (do
      (.close fc)
      (.close fis))))

(def ^:private map-modes
  {:private    FileChannel$MapMode/PRIVATE
   :read-only  FileChannel$MapMode/READ_ONLY
   :read-write FileChannel$MapMode/READ_WRITE})

(def ^:private map-perms
  {:private    "r"
   :read-only  "r"
   :read-write "rw"})

(defn get-mmap
  "Provided a file, mmap the entire file, and return an opaque type to allow further access.
   Remember to use with-open, or to call .close, to clean up memory and open file descriptors.
   The file argument can be any implementation of clojure.java.io/Coercions."
  ([file] (get-mmap file :read-only))
  ([file map-mode]
   (let [fis  (RandomAccessFile.
                ^File (io/as-file file)
                (str (map-perms map-mode)))
         fc   (.getChannel fis)
         size (.size fc)
         mmap (fn [pos n] (.map fc (map-modes map-mode) pos n))]
     (Mmap. fis fc (mapv #(mmap % (min (- size %)
                                       bytes-per-map))
                         (range 0 size bytes-per-map)))))
  ([file map-mode size]
   (let [fis  (RandomAccessFile.
                ^File (io/as-file file)
                (str (map-perms map-mode)))
         fc   (.getChannel fis)
         mmap (fn [pos n] (.map fc (map-modes map-mode) pos n))]
     (Mmap. fis fc (mapv #(mmap % (min (- size %)
                                       bytes-per-map))
                         (range 0 size bytes-per-map))))))

(defn get-bytes ^bytes [mmap pos n]
  "Retrieve n bytes from mmap, at byte position pos."
  (let [get-chunk   #(nth mmap (int (/ % bytes-per-map)))
        end         (+ pos n)
        chunk-term  (-> pos
                        (/ bytes-per-map)
                        int
                        inc
                        (* bytes-per-map))
        read-size   (- (min end chunk-term) ;; bytes to read in first chunk
                       pos)
        start-chunk ^MappedByteBuffer (get-chunk pos)
        end-chunk   ^MappedByteBuffer (get-chunk end)
        buf         (byte-array n)]

    (locking start-chunk
      (.position start-chunk (mod pos bytes-per-map))
      (.get start-chunk buf 0 read-size))

    ;; Handle reads that span MappedByteBuffers
    (if (not= start-chunk end-chunk)
      (locking end-chunk
        (.position end-chunk 0)
        (.get end-chunk buf read-size (- n read-size))))

    buf))

(defn put-bytes
  "Write n bytes from buf into mmap, at byte position pos.
   If n isn't provided, the size of the buffer provided is used."
  ([mmap ^bytes buf pos] (put-bytes mmap buf pos (alength buf)))
  ([mmap ^bytes buf pos n]
   (let [get-chunk   #(nth mmap (int (/ % bytes-per-map)))
         end         (+ pos n)
         chunk-term  (-> pos
                         (/ bytes-per-map)
                         int
                         inc
                         (* bytes-per-map))
         write-size   (- (min end chunk-term)
                         pos)
         start-chunk ^MappedByteBuffer (get-chunk pos)
         end-chunk   ^MappedByteBuffer (get-chunk end)]

     (assert (some? start-chunk))

     (locking start-chunk
       (doto start-chunk
         (.position (mod pos bytes-per-map))
         (.put buf 0 write-size)))

     ;; Handle writes that span MappedByteBuffers
     (if (not= start-chunk end-chunk)
       (locking end-chunk
         (.position end-chunk 0)
         (.put end-chunk buf write-size (- n write-size))))

     nil)))

(defn loaded? [mmap]
  "Returns true if it is likely that the buffer's contents reside in physical memory."
  (every? (fn [^MappedByteBuffer buf]
            (.isLoaded buf))
          mmap))
