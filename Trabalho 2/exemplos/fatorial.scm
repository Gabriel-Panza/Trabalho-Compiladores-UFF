(define (fat n)
  (if (= n 0)
      1
      (* n (fat (- n 1)))))

(display (fat 5))
(newline)
