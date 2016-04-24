/* Example field template in the Public Domain */

$if (f.optional)$

  /** Returns whether $f.name$ has been set. */
  boolean has$f.capName$();

  /** Clears the value of $f.name$. */
  void clear$f.capName$();

$endif$

$if (f.repeated)$

  /** Returns $f.name$, or null if hasn't been set. */
  $if (f.message)$
    List<? extends $f.javaType$> $f.getter$();
  $else$
    List<$f.boxedJavaType$> $f.getter$();
  $endif$

  /** Adds an element to $f.name$. */
  void add$f.capName$($f.javaType$ value);

  /** Adds a list of elements to $f.name$. */
  $if (f.message)$
    void addAll$f.capName$(List<? extends $f.javaType$> $f.name$);
  $else$
    void addAll$f.capName$(List<$f.boxedJavaType$> $f.name$);
  $endif$

  /** Returns the nth element of $f.name$. */
  $f.javaType$ $f.getter$(int n);

  /** Sets the nth element of $f.name$. */
  void $f.setter$(int n, $f.javaType$ value);

  /** Returns the length of $f.name$. */
  int $f.getter$Size();

  /** Clears $f.name$. */
  void clear$f.capName$();

$else$

  /** Returns $f.name$, or null if hasn't been set. */
  $f.javaType$ $f.getter$();

  /** Sets $f.name$. */
  void $f.setter$($f.javaType$ $f.name$);

$endif$
