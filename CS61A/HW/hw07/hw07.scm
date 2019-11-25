(define (cddr s)
  (cdr (cdr s)))

(define (cadr s)
  (Car (cdr s))
)

(define (caddr s)
  (Car (cdr (cdr s)))
)

(define (sign x)
  (cond ((= x 0) 0)
		((> x 0) 1)
		(else -1)		

)
)

(define (square x) (* x x))

(define (pow b n)
  (cond ((= n 0) 1)
	((even? n) (* (pow b (/ n 2)) (pow b (/ n 2))))
	(else (* b (* (pow b (/ (- n 1) 2)) (pow b (/ (- n 1) 2)))))

)
)

(define (ordered? s)
  (cond ((null? (cdr s)) #t)
		((> (car s) (cadr s)) #f)
		(else (ordered? (Cdr s)))

)
)