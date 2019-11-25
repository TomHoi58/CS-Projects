class Carter:
  def __str__(self):
    return "str"
  def __repr__(self):
    return "repr"

class CarterNoStr:
  def __repr__(self):
    return "repr"

class CarterNoRepr:
  def __str__(self):
    return "str"

class CarterNoNothing:
  c = 0

> c = Carter()
> c
	repr
> print(c)
	str
> str(c)
	'str'
> cns = CarterNoStr()
> cns
	repr
> print(cns)
	repr
> str(cns)
	'repr'
> cnr = CarterNoRepr()
> cnr
	<__main__.CarterNoRepr object at 0x7f46fc0266a0>
> print(cnr)
	str
> str(cnr)
	'str'
> cnn = CarterNoNothing()
> cnn
	<__main__.CarterNoNothing object at 0x7f46fc0267b8>
> print(cnn)
	<__main__.CarterNoNothing object at 0x7f46fc0267b8>
> str(cnn)
	'<__main__.CarterNoNothing object at 0x7f46fc0267b8>'