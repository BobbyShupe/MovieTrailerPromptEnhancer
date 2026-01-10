package com.example.promptstyle

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var basePrompt: EditText
    private lateinit var result: TextView
    private lateinit var musicIntensitySpinner: Spinner
    private lateinit var presetSpinner: Spinner
    private lateinit var presetNameInput: EditText

    private val genreChecks = mutableListOf<CheckBox>()
    private val styleChecks = mutableListOf<CheckBox>()
    private val animStyleChecks = mutableListOf<CheckBox>()
    private val musicChecks = mutableListOf<CheckBox>()
    private val musicMoodChecks = mutableListOf<CheckBox>()

    private val gson = Gson()
    private val presets = mutableMapOf<String, PresetData>()
    private val presetNames = mutableListOf<String>()

    data class PresetData(
        val genres: List<Boolean>,
        val styles: List<Boolean>,
        val animStyles: List<Boolean>,
        val musicIntensityIndex: Int,
        val musicDetails: List<Boolean>,
        val musicMood: List<Boolean>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        basePrompt = findViewById(R.id.base_prompt)
        result = findViewById(R.id.result)
        musicIntensitySpinner = findViewById(R.id.music_intensity_spinner)
        presetSpinner = findViewById(R.id.preset_spinner)
        presetNameInput = findViewById(R.id.preset_name_input)

        // Initialize checkbox lists
        genreChecks.addAll(listOf(
            findViewById(R.id.genre_action), findViewById(R.id.genre_horror), findViewById(R.id.genre_comedy),
            findViewById(R.id.genre_romance), findViewById(R.id.genre_sci_fi), findViewById(R.id.genre_animation)
        ))

        styleChecks.addAll(listOf(
            findViewById(R.id.style_epic), findViewById(R.id.style_big_eyed), findViewById(R.id.style_dark),
            findViewById(R.id.style_funny), findViewById(R.id.style_emotional)
        ))

        animStyleChecks.addAll(listOf(
            findViewById(R.id.anim_wholesome), findViewById(R.id.anim_fairytale),
            findViewById(R.id.anim_bright_colors), findViewById(R.id.anim_soft_lighting),
            findViewById(R.id.anim_sweeping_camera), findViewById(R.id.anim_talking_animal),
            findViewById(R.id.anim_slapstick), findViewById(R.id.anim_emotional_kids)
        ))

        musicChecks.addAll(listOf(
            findViewById(R.id.music_braams), findViewById(R.id.music_hybrid_orchestral),
            findViewById(R.id.music_risers_hits), findViewById(R.id.music_epic_choir),
            findViewById(R.id.music_pulsing_synth), findViewById(R.id.music_horror_stingers),
            findViewById(R.id.music_quirky_orchestra), findViewById(R.id.music_heartwarming_piano)
        ))

        // Music Mood (updated with separate Tense, Ominous, Mysterious, etc.)
        musicMoodChecks.addAll(listOf(
            findViewById(R.id.music_dramatic_orchestral),
            findViewById(R.id.music_suspenseful),
            findViewById(R.id.music_tense),
            findViewById(R.id.music_ominous),
            findViewById(R.id.music_mysterious),
            findViewById(R.id.music_pensive),
            findViewById(R.id.music_heartwarming),
            findViewById(R.id.music_dark_ambient),
            findViewById(R.id.music_slow),
            findViewById(R.id.music_melancholy),
            findViewById(R.id.music_somber),
            findViewById(R.id.music_intriguing),
            findViewById(R.id.music_percussive)
        ))

        // Setup spinners
        ArrayAdapter.createFromResource(
            this,
            R.array.music_intensity_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            musicIntensitySpinner.adapter = adapter
        }

        // Load presets
        loadPresets()

        // Buttons
        findViewById<Button>(R.id.generate_button).setOnClickListener { generateEnhancedPrompt() }
        findViewById<Button>(R.id.save_preset_button).setOnClickListener { savePresetWithConfirmation() }
        findViewById<Button>(R.id.delete_preset_button).setOnClickListener { deletePreset() }
        findViewById<Button>(R.id.clear_button).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear All Selections?")
                .setMessage("This will reset all checkboxes, spinners, and the base prompt. Continue?")
                .setPositiveButton("Yes, Clear") { _, _ ->
                    clearAllSelections()
                    Toast.makeText(this, "All selections cleared!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) {
                    val presetName = presetNames[position]
                    loadPreset(presets[presetName]!!)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun clearAllSelections() {
        genreChecks.forEach { it.isChecked = false }
        styleChecks.forEach { it.isChecked = false }
        animStyleChecks.forEach { it.isChecked = false }
        musicChecks.forEach { it.isChecked = false }
        musicMoodChecks.forEach { it.isChecked = false }

        musicIntensitySpinner.setSelection(0)

        basePrompt.text.clear()
        result.text = ""

        presetSpinner.setSelection(0)
    }

    private fun savePresetWithConfirmation() {
        val name = presetNameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a preset name", Toast.LENGTH_SHORT).show()
            return
        }

        if (presets.containsKey(name)) {
            AlertDialog.Builder(this)
                .setTitle("Overwrite Preset?")
                .setMessage("A preset named '$name' already exists. Do you want to overwrite it?")
                .setPositiveButton("Yes, Overwrite") { _, _ ->
                    savePreset(name)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            savePreset(name)
        }
    }

    private fun savePreset(name: String) {
        val data = PresetData(
            genres = genreChecks.map { it.isChecked },
            styles = styleChecks.map { it.isChecked },
            animStyles = animStyleChecks.map { it.isChecked },
            musicIntensityIndex = musicIntensitySpinner.selectedItemPosition,
            musicDetails = musicChecks.map { it.isChecked },
            musicMood = musicMoodChecks.map { it.isChecked }
        )

        presets[name] = data
        savePresetsToStorage()
        updatePresetSpinner()
        presetNameInput.text.clear()
        Toast.makeText(this, "Preset '$name' saved!", Toast.LENGTH_SHORT).show()
    }

    private fun deletePreset() {
        val selected = presetSpinner.selectedItemPosition
        if (selected <= 0) {
            Toast.makeText(this, "Select a preset to delete", Toast.LENGTH_SHORT).show()
            return
        }
        val name = presetNames[selected]
        presets.remove(name)
        savePresetsToStorage()
        updatePresetSpinner()
        Toast.makeText(this, "Preset '$name' deleted", Toast.LENGTH_SHORT).show()
    }

    private fun loadPreset(data: PresetData) {
        genreChecks.forEachIndexed { i, cb -> cb.isChecked = data.genres.getOrElse(i) { false } }
        styleChecks.forEachIndexed { i, cb -> cb.isChecked = data.styles.getOrElse(i) { false } }
        animStyleChecks.forEachIndexed { i, cb -> cb.isChecked = data.animStyles.getOrElse(i) { false } }
        musicIntensitySpinner.setSelection(data.musicIntensityIndex)
        musicChecks.forEachIndexed { i, cb -> cb.isChecked = data.musicDetails.getOrElse(i) { false } }
        musicMoodChecks.forEachIndexed { i, cb -> cb.isChecked = data.musicMood.getOrElse(i) { false } }
    }

    private fun loadPresets() {
        val prefs = getSharedPreferences("presets", Context.MODE_PRIVATE)
        val json = prefs.getString("preset_data", null)
        if (json != null) {
            val typeToken = object : TypeToken<Map<String, PresetData>>() {}.type
            presets.putAll(gson.fromJson(json, typeToken))
        }
        updatePresetSpinner()
    }

    private fun savePresetsToStorage() {
        val prefs = getSharedPreferences("presets", Context.MODE_PRIVATE)
        prefs.edit().putString("preset_data", gson.toJson(presets)).apply()
    }

    private fun updatePresetSpinner() {
        presetNames.clear()
        presetNames.add("— Load Preset —")
        presetNames.addAll(presets.keys.sorted())

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presetNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        presetSpinner.adapter = adapter
        presetSpinner.setSelection(0)
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

        // Animation style details
        if (findViewById<CheckBox>(R.id.anim_wholesome).isChecked) {
            enhancements.append(" Wholesome family-friendly tone suitable for all ages.")
        }
        if (findViewById<CheckBox>(R.id.anim_fairytale).isChecked) {
            enhancements.append(" Set in a magical fairy-tale kingdom with wonder and enchantment.")
        }
        if (findViewById<CheckBox>(R.id.anim_bright_colors).isChecked) {
            enhancements.append(" Bright and saturated colors with a vibrant, colorful world.")
        }
        if (findViewById<CheckBox>(R.id.anim_soft_lighting).isChecked) {
            enhancements.append(" Soft, gentle lighting creating a warm and inviting atmosphere.")
        }
        if (findViewById<CheckBox>(R.id.anim_sweeping_camera).isChecked) {
            enhancements.append(" Sweeping cinematic camera moves and dynamic angles.")
        }
        if (findViewById<CheckBox>(R.id.anim_talking_animal).isChecked) {
            enhancements.append(" Featuring a lovable talking animal sidekick.")
        }
        if (findViewById<CheckBox>(R.id.anim_slapstick).isChecked) {
            enhancements.append(" Gentle slapstick humor and lighthearted comedy.")
        }
        if (findViewById<CheckBox>(R.id.anim_emotional_kids).isChecked) {
            enhancements.append(" Emotional moments that resonate with both kids and parents.")
        }

        // Music intensity
        val intensity = musicIntensitySpinner.selectedItem.toString()
        musicDetails.append(" Use powerful trailer music with $intensity intensity")

        // Music details
        val selectedMusic = musicChecks.filter { it.isChecked }.map { it.text.toString() }
        if (selectedMusic.isNotEmpty()) {
            musicDetails.append(", featuring ${selectedMusic.joinToString(", ")}")
        }

        // Music mood
        val selectedMood = musicMoodChecks.filter { it.isChecked }.map { it.text.toString() }
        if (selectedMood.isNotEmpty()) {
            musicDetails.append(", with ${selectedMood.joinToString(" and ")}")
        }
        musicDetails.append(".")

        if (selectedMusic.isEmpty() && selectedMood.isEmpty()) {
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