/* Example Protobuffer Class template in the Public Domain */

public interface $m.javaType$ {

  $m.nestedEnums: {e|$enum(e=e)$}$
  $m.nestedMessages: {nested|$interface(m=nested)$}$

  ///** Does a deep copy from model. */
  void copyFrom($m.javaType$ model);

  /**
   * Tests if this model is equal to another object.
   * "Equal" is recursively defined as:
   * <ul>
   * <li>both objects implement this interface,</li>
   * <li>all corresponding primitive fields of both objects have the same value, and</li>
   * <li>all corresponding nested-model fields of both objects are "equal".</li>
   * </ul>
   *
   * This is a coarser equivalence than provided by the equals() methods.  Two
   * objects may not be equal() to each other, but may be isEqualTo() each other.
   */
  boolean isEqualTo(Object o);

  $m.fields: {f|$beans(f=f)$}$
}
