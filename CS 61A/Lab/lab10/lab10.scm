;; Scheme ;;

(define (repeatedly-cube n x)
    (if (zero? n)
        x
        (let
            ((Y (repeatedly-cube (- n 1) x)))
            (* y y y))))