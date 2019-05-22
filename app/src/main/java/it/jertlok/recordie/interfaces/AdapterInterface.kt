/*
 *     This file is part of Recordie.
 *
 *     Recordie is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Recordie is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Recordie.  If not, see <https://www.gnu.org/licenses/>.
 */

package it.jertlok.recordie.interfaces

interface AdapterInterface {
    // Called when we click on delete button
    fun deleteEvent(videoData: String)

    // Called when we click on play button
    fun playVideo(videoData: String)

    // Called when we click on share button
    fun shareVideo(videoData: String)

    // Called when we long click the card view
    fun updateCardCheck()
}