(define (caar x) (car (car x)))
(define (cadr x) (car (cdr x)))
(define (cdar x) (cdr (car x)))
(define (cddr x) (cdr (cdr x)))

; Some utility functions that you may find useful to implement.

(define (cons-all first rests)
  (map (lambda (x) (cons first x)) rests)
)


(define (zip pairs) 
   (cond ((null? pairs) '(()()))
         (else (cons (cons (car (car pairs)) (car (zip (cdr pairs)))) (cons (cons (car (cdr (car pairs))) (car (cdr (zip (cdr pairs))))) nil))
             
             )))

;; Problem 17
;; Returns a list of two-element lists
(define (enumerate s)
  ; BEGIN PROBLEM 17
  (define (enumerate-helper s n)
      (cond ((null? s) s)
            (else (cons (cons n (cons (car s) nil)) (enumerate-helper (cdr s) (+ n 1))
          ))
      ))
  (enumerate-helper s 0)
)
  ; END PROBLEM 17

;; Problem 18
;; List all ways to make change for TOTAL with DENOMS
(define (list-change total denoms)
  ; BEGIN PROBLEM 18
  (cond ((= total 0) '(()))
        ((null? denoms) nil)
        ((< total (car denoms)) (list-change total (cdr denoms)))
        (else (append (cons-all (car denoms) (list-change (- total (car denoms)) denoms)) (list-change total (cdr denoms))))
        )
)

  ; END PROBLEM 18

;; Problem 19
;; Returns a function that checks if an expression is the special form FORM
(define (check-special form)
  (lambda (expr) (equal? form (car expr))))

(define lambda? (check-special 'lambda))
(define define? (check-special 'define))
(define quoted? (check-special 'quote))
(define let?    (check-special 'let))

;; Converts all let special forms in EXPR into equivalent forms using lambda
(define (let-to-lambda expr)
  (cond 
    ((atom? expr)
     ; BEGIN PROBLEM 19
     expr
     ; END PROBLEM 19
    )
    ((quoted? expr)
     ; BEGIN PROBLEM 19
     'replace-this-line
     expr
     ; END PROBLEM 19
    )
    ((or (lambda? expr) (define? expr))
     (let ((form (car expr))
           (params (cadr expr))
           (body (cddr expr))
          )
       ; BEGIN PROBLEM 19
       (cons form (cons params (map let-to-lambda body)))
       ; END PROBLEM 19
     )
    )
    ((let? expr)
     (let ((values (cadr expr))
           (body (cddr expr))
          )
       ; BEGIN PROBLEM 19
       (cons (cons 'lambda (cons (car (zip values)) (map let-to-lambda body))) (map let-to-lambda (car (cdr (zip values)))))
		
       ; END PROBLEM 19
     )
    )
    (else
     ; BEGIN PROBLEM 19
     (Map let-to-lambda Expr)
     ; END PROBLEM 19
    )
  )
)
