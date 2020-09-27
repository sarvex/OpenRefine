package org.openrefine.browsing.util;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.openrefine.browsing.facets.ScatterplotFacet;
import org.openrefine.browsing.facets.ScatterplotFacet.Dimension;
import org.openrefine.browsing.facets.ScatterplotFacet.Rotation;
import org.openrefine.browsing.filters.DualExpressionsNumberComparisonRowFilter;

/**
 * Filters rows according to the settings of a scatterplot facet.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ScatterplotRowFilter extends DualExpressionsNumberComparisonRowFilter {

	private static final long serialVersionUID = -1872143237020444678L;
	
	private final double    _fromX;
	private final double    _toX;
	private final double    _fromY;
	private final double    _toY;
	private final Dimension _dimX;
	private final Dimension _dimY;
	private final double    _l;
	private final Rotation  _rotation;
	
	private transient AffineTransform _transform;

	public ScatterplotRowFilter(
			RowEvaluable evaluableX,
			RowEvaluable evaluableY,
			double fromX,
			double toX,
			double fromY,
			double toY,
			Dimension dimX,
			Dimension dimY,
			double l,
			Rotation rotation) {
		super(evaluableX, evaluableY);
		_fromX = fromX;
		_toX = toX;
		_fromY = fromY;
		_toY = toY;
		_dimX = dimX;
		_dimY = dimY;
		_l = l;
		_rotation = rotation;
	}

	@Override
	protected boolean checkValues(double x, double y) {
		Point2D.Double point = new Point2D.Double(x, y);
		Point2D.Double t = ScatterplotFacet.translateCoordinates(point, _dimX, _dimY, _l, getTransform());
        return t.x >= _fromX && t.x <= _toX && t.y >= _fromY && t.y <= _toY;
	}

	private AffineTransform getTransform() {
		if (_transform != null || _rotation == Rotation.NO_ROTATION) {
			return _transform;
		}
		_transform = ScatterplotFacet.createRotationMatrix(_rotation, _l);
		return _transform;
	}

}
