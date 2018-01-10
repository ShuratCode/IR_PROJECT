package Tuple;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Shaked
 * @since 06-Dec-17
 */
public class MutableTriple<L, M, R> implements Serializable
{


    private L left;
    private R right;
    private M middle;


    public MutableTriple(L left, M middle, R right)
    {
        this.left = left;
        this.right = right;
        this.middle = middle;
    }

    /**
     * @return left of this triple
     */
    public L getLeft()
    {
        return this.left;
    }

    /**
     * @param left set new value to the left of this triple
     */
    public void setLeft(L left)
    {
        this.left = left;
    }

    /**
     * @return the right of this triple
     */
    public R getRight()
    {
        return this.right;
    }

    /**
     * @param right new value for the right of this triple
     */
    public void setRight(R right)
    {
        this.right = right;
    }

    /**
     * @return the middle of this triple
     */
    public M getMiddle()
    {
        return this.middle;
    }

    /**
     * @param middle new value for this triple middle
     */
    public void setMiddle(M middle)
    {
        this.middle = middle;
    }

    @Override
    public int hashCode()
    {
        return (this.left == null ? 0 : this.left.hashCode() ^ (this.middle == null ? 0 : this.middle.hashCode() ^ (this.right == null ? 0 : this.right.hashCode())));
    }

    @Override
    public String toString()
    {
        return this.left + "," + this.middle + "," + this.right;
    }

    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof MutableTriple))
        {
            return false;
        }
        MutableTriple<L, M, R> other = (MutableTriple<L, M, R>) obj;
        return Objects.equals(this.getLeft(), other.getLeft()) && Objects.equals(this.getMiddle(), other.getMiddle()) && Objects.equals(this.getRight(), other.getRight());
    }
}
