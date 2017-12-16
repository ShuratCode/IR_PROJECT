package Tuple;

import java.io.Serializable;

/**
 * @author Shaked
 * @since 07-Dec-17
 */
public class MutablePair<L, R> implements Serializable
{
  private L left;
  private R right;

  public MutablePair (L left, R right)
  {
    this.left = left;
    this.right = right;
  }

  public L getLeft ()
  {
    return left;
  }

  public void setLeft (L left)
  {
    this.left = left;
  }

  public R getRight ()
  {
    return right;
  }

  public void setRight (R right)
  {
    this.right = right;
  }

  @Override
  public boolean equals (Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (o == null || getClass() != o.getClass())
    {
      return false;
    }

    MutablePair<?, ?> that = (MutablePair<?, ?>) o;

    if (left != null ? !left.equals(that.left) : that.left != null)
    {
      return false;
    }
    return right != null ? right.equals(that.right) : that.right == null;
  }

  @Override
  public int hashCode ()
  {
    int result = left != null ? left.hashCode() : 0;
    result = 31 * result + (right != null ? right.hashCode() : 0);
    return result;
  }

  @Override
  public String toString ()
  {
    return "MutablePair{" + "left=" + left + ", right=" + right + '}';
  }
}
