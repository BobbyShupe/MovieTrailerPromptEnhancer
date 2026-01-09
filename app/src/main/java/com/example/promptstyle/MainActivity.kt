package com.example.promptstyle

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var basePrompt: EditText
    private lateinit var result: TextView
    private lateinit var musicIntensitySpinner: Spinner

    private val genreChecks = mutableListOf<CheckBox>()
    private val styleChecks = mutableListOf<CheckBox>()
    private val musicChecks = mutableListOf<CheckBox>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        basePrompt = findViewById(R.id.base_prompt)
        result = findViewById(R.id.result)
        musicIntensitySpinner = findViewById(R.id.music_intensity_spinner)

        // Genre checkboxes
        genreChecks.add(findViewById(R.id.genre_action))
        genreChecks.add(findViewById(R.id.genre_horror))
        genreChecks.add(findViewById(R.id.genre_comedy))
        genreChecks.add(findViewById(R.id.genre_romance))
        genreChecks.add(findViewById(R.id.genre_sci_fi))
        genreChecks.add(findViewById(R.id.genre_animation))

        // Style checkboxes
        styleChecks.add(findViewById(R.id.style_epic))
        styleChecks.add(findViewById(R.id.style_big_eyed))
        styleChecks.add(findViewById(R.id.style_dark))
        styleChecks.add(findViewById(R.id.style_funny))
        styleChecks.add(findViewById(R.id.style_emotional))

        // Music checkboxes
        musicChecks.add(findViewById(R.id.music_braams))
        musicChecks.add(findViewById(R.id.music_hybrid_orchestral))
        musicChecks.add(findViewById(R.id.music_risers_hits))
        musicChecks.add(findViewById(R.id.music_epic_choir))
        musicChecks.add(findViewById(R.id.music_pulsing_synth))
        musicChecks.add(findViewById(R.id.music_horror_stingers))
        musicChecks.add(findViewById(R.id.music_quirky_orchestra))
        musicChecks.add(findViewById(R.id.music_heartwarming_piano))

        // Setup spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.music_intensity_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            musicIntensitySpinner.adapter = adapter
        }

        val generateButton: Button = findViewById(R.id.generate_button)
        generateButton.setOnClickListener { generateEnhancedPrompt() }
    }

    private fun generateEnhancedPrompt() {
        var base = basePrompt.text.toString().trim()
        if (base.isEmpty()) base = "A thrilling movie"

        val selectedGenres = genreChecks.filter { it.isChecked }.map { it.text.toString() }
        if (selectedGenres.isNotEmpty()) {
            base += " in the " + selectedGenres.joinToString(", ") + " genre"
        }

        val enhancements = StringBuilder()
        val musicDetails = StringBuilder()

        // Visual styles
        if (findViewById<CheckBox>(R.id.style_epic).isChecked) {
            enhancements.append(" Epic cinematic visuals with sweeping camera moves, dramatic lighting, and high-stakes action.")
        }
        if (findViewById<CheckBox>(R.id.style_big_eyed).isChecked) {
            enhancements.append(" Adorable 3D animated characters with big expressive eyes, vibrant colors, in a cute animated style.")
        }
        if (findViewById<CheckBox>(R.id.style_dark).isChecked) {
            enhancements.append(" Dark, gritty atmosphere with shadowy visuals and intense mood.")
        }
        if (findViewById<CheckBox>(R.id.style_funny).isChecked) {
            enhancements.append(" Humorous, quirky tone with witty timing and exaggerated expressions.")
        }
        if (findViewById<CheckBox>(R.id.style_emotional).isChecked) {
            enhancements.append(" Emotional, heartwarming moments with touching close-ups and slow-motion feels.")
        }

        // Music
        val intensity = musicIntensitySpinner.selectedItem.toString()
        musicDetails.append(" Use powerful trailer music with $intensity intensity")

        val selectedMusic = musicChecks.filter { it.isChecked }.map { it.text.toString() }
        if (selectedMusic.isNotEmpty()) {
            musicDetails.append(", featuring ${selectedMusic.joinToString(", ")}")
        }
        musicDetails.append(".")

        if (selectedMusic.isEmpty()) {
            musicDetails.append(" featuring epic orchestral swells, deep braams, and dramatic risers.")
        }

        val enhancedPrompt = """
            Generate a compelling movie trailer voiceover script and visual description for: $base.
            Make it exciting and cinematic.$enhancements
            $musicDetails
            Structure with building tension, quick cuts, deep impactful narration, and a powerful final tagline.
        """.trimIndent()

        result.text = enhancedPrompt
    }
}