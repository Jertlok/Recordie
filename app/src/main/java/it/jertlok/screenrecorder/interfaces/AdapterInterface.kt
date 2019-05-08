package it.jertlok.screenrecorder.interfaces

interface AdapterInterface {
    // Called when we click on delete button
    fun deleteEvent(videoData: String)

    // Called when we click on play button
    fun playVideo(videoData: String)

    // Called when we click on share button
    fun shareVideo(videoData: String)
}