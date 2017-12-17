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

    public MutablePair(L left, R right)
    {
        this.left = left;
        this.right = right;
    }

    /**
     * @return the left of this pair
     */
    public L getLeft()
    {
        return left;
    }

    /**
     * @param left new value for this left
     */
    public void setLeft(L left)
    {
        this.left = left;
    }

    /**
     * @return the right of this pair
     */
    public R getRight()
    {
        return right;
    }

    /**
     * @param right new value for this right
     */
    public void setRight(R right)
    {
        this.right = right;
    }


    @Override
    public boolean equals(Object o)
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
    public int hashCode()
    {
        int result = left != null ? left.hashCode() : 0;
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "!#" + this.left + "!#" + this.right;
    }


}
