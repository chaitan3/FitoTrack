/*
 * Copyright (c) 2026 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

// Copied from https://github.com/sisyphsu/dateparser (MIT License)
package de.tadris.fitness.util.dateparser;

import com.github.sisyphsu.retree.ReMatcher;

/**
 * This class represents the standard specification of rule's handler.
 *
 * @author sulin
 * @since 2019-09-14 14:25:49
 */
@FunctionalInterface
public interface RuleHandler {

    void handle(CharSequence chars, ReMatcher matcher, DateBuilder dt);

}
