/*
ExpandedBonemeal Minecraft Mod
Copyright (C) 2016 Joseph C. Sible

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package josephcsible.expandedbonemeal;

import java.util.Random;

/**
 * Always gives 0 for the first answer to nextInt(n), then goes back to being random.
 */
public class ZeroFirstIntRandom extends Random {
	protected boolean firstTime = true;
	@Override
	public int nextInt(int n) {
		if(!firstTime)
			return super.nextInt(n);
		firstTime = false;
		return 0;
	}
}