package com.horstmann.violet.product.diagram.abstracts.edge;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.horstmann.violet.product.diagram.abstracts.ISelectable;

public class EdgeTransitionPoint implements ITransitionPoint {

	private double x = -1;
	private double y = -1;

	public EdgeTransitionPoint(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public void setX(double x) {
		this.x = x;
	}

	@Override
	public void setY(double y) {
		this.y = y;
	}

	@Override
	public double getX() {
		return this.x;
	}

	@Override
	public double getY() {
		return this.y;
	}

	public static ITransitionPoint fromPoint2D(Point2D p) {
		ITransitionPoint t = new EdgeTransitionPoint(p.getX(), p.getY());
		return t;
	}

	/**
	 * Deep copy, so that the copy doesn't move with the original points (e.g. to
	 * keep them in the undo/redo history)
	 */
	public static ITransitionPoint[] copyOf(ITransitionPoint[] transitionPoints) {
		if (transitionPoints == null) {
			return new ITransitionPoint[] {};
		}
		return Arrays.stream(transitionPoints).map(p -> fromPoint2D(p.toPoint2D())).toArray(ITransitionPoint[]::new);
	}

	@Override
	public Point2D toPoint2D() {
		return new Point2D.Double(this.x, this.y);
	}

	@Override
	public List<Point2D> getSelectionPoints() {
		return Arrays.asList(this.toPoint2D());
	}

	
	@Override
	public ISelectable getSelectableParent() {
		return null;
	}
		
	@Override
	public List<ISelectable> getSelectableChildren() {
		return new ArrayList<>();
	}
	


	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EdgeTransitionPoint other = (EdgeTransitionPoint) obj;
		return Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x)
				&& Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y);
	}

}
