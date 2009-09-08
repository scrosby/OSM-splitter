package uk.me.parabola.splitter;

import java.util.List;

public interface SplittableArea {
	/**
	 * @return the area that this splittable area represents
	 */
	Area getBounds();

	/**
	 * @param maxNodes the maximum number of nodes per area
	 * @return a list of areas, each containing no more than {@code maxNodes} nodes.
	 * Each area returned must be aligned to the appropriate overview map resolution.
	 */
	List<Area> split(int maxNodes);
}
