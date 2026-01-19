package com.example.promptstyle

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var basePrompt: EditText
    private lateinit var result: TextView
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

    private lateinit var prefs: SharedPreferences

    data class PresetData(
        val genres: List<Boolean>,
        val styles: List<Boolean>,
        val animStyles: List<Boolean>,
        val musicDetails: List<Boolean>,
        val musicMood: List<Boolean>
    )

    data class UiState(
        val basePromptText: String = "",
        val presetNameText: String = "",
        val selectedPresetIndex: Int = 0,
        val genreStates: List<Boolean> = emptyList(),
        val styleStates: List<Boolean> = emptyList(),
        val animStyleStates: List<Boolean> = emptyList(),
        val musicDetailStates: List<Boolean> = emptyList(),
        val musicMoodStates: List<Boolean> = emptyList()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("PromptStylePrefs", Context.MODE_PRIVATE)

        basePrompt = findViewById(R.id.base_prompt)
        result = findViewById(R.id.result)
        presetSpinner = findViewById(R.id.preset_spinner)
        presetNameInput = findViewById(R.id.preset_name_input)

        genreChecks.addAll(listOf(
            findViewById(R.id.genre_action),
            findViewById(R.id.genre_horror),
            findViewById(R.id.genre_comedy),
            findViewById(R.id.genre_romance),
            findViewById(R.id.genre_sci_fi),
            findViewById(R.id.genre_animation)
        ))

        styleChecks.addAll(listOf(
            findViewById(R.id.style_epic),
            findViewById(R.id.style_big_eyed),
            findViewById(R.id.style_dark),
            findViewById(R.id.style_funny),
            findViewById(R.id.style_emotional),
            findViewById(R.id.style_cinematic)
        ))

        animStyleChecks.addAll(listOf(
            findViewById(R.id.anim_wholesome),
            findViewById(R.id.anim_fairytale),
            findViewById(R.id.anim_bright_colors),
            findViewById(R.id.anim_soft_lighting),
            findViewById(R.id.anim_sweeping_camera),
            findViewById(R.id.anim_talking_animal),
            findViewById(R.id.anim_talking_animal_noraccoon),
            findViewById(R.id.anim_slapstick),
            findViewById(R.id.anim_emotional_kids)
        ))

        musicChecks.addAll(listOf(
            findViewById(R.id.music_braams),
            findViewById(R.id.music_hybrid_orchestral),
            findViewById(R.id.music_risers_hits),
            findViewById(R.id.music_epic_choir),
            findViewById(R.id.music_pulsing_synth),
            findViewById(R.id.music_horror_stingers),
            findViewById(R.id.music_quirky_orchestra),
            findViewById(R.id.music_heartwarming_piano)
        ))

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
            findViewById(R.id.music_anxiety_unease),
            findViewById(R.id.music_surprise),
            findViewById(R.id.music_dreaminess),
            findViewById(R.id.music_nostalgia),
            findViewById(R.id.music_ambivalence_conflict),
            findViewById(R.id.music_ambiguous),
            findViewById(R.id.music_mixed_feelings),
            findViewById(R.id.music_bittersweet),
            findViewById(R.id.music_uncertain_unresolved),
            findViewById(R.id.music_surreal),
            findViewById(R.id.music_disoriented),
            findViewById(R.id.music_restless),
            findViewById(R.id.music_happy),
            findViewById(R.id.music_energetic),
            findViewById(R.id.music_uplifting),
            findViewById(R.id.music_feelgood),
            findViewById(R.id.music_cheerful),
            findViewById(R.id.music_optimistic),
            findViewById(R.id.music_playful),
            findViewById(R.id.music_bright),
            findViewById(R.id.music_upbeat)
        ))

        loadPresets()
        restoreUiState()

        findViewById<Button>(R.id.generate_button).setOnClickListener { generateEnhancedPrompt() }
        findViewById<Button>(R.id.save_preset_button).setOnClickListener { savePresetWithConfirmation() }
        findViewById<Button>(R.id.delete_preset_button).setOnClickListener { deletePreset() }

        findViewById<Button>(R.id.copy_button).setOnClickListener {
            val textToCopy = result.text.toString()
            if (textToCopy.isNotEmpty()) {
                try {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Trailer Prompt", textToCopy)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Copy failed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Nothing to copy", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.clear_button).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear All?")
                .setMessage("Reset checkboxes, base prompt, preset name and result?")
                .setPositiveButton("Yes") { _, _ -> clearAllSelections() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position > 0) {
                    val name = presetNames[position]
                    loadPreset(presets[name]!!)
                    presetNameInput.setText(name)  // optional: show loaded preset name
                    saveUiState()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val checkListener = CompoundButton.OnCheckedChangeListener { _, _ -> saveUiState() }
        (genreChecks + styleChecks + animStyleChecks + musicChecks + musicMoodChecks).forEach {
            it.setOnCheckedChangeListener(checkListener)
        }

        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveUiState()
            }
        }

        basePrompt.addTextChangedListener(textWatcher)
        presetNameInput.addTextChangedListener(textWatcher)   // ← this is required for preset name
    }

    override fun onPause() {
        super.onPause()
        saveUiState()
    }

    private fun saveUiState() {
        val state = UiState(
            basePromptText = basePrompt.text.toString(),
            presetNameText = presetNameInput.text.toString(),
            selectedPresetIndex = presetSpinner.selectedItemPosition.coerceIn(0, presetSpinner.adapter?.count ?: 1),
            genreStates = genreChecks.map { it.isChecked },
            styleStates = styleChecks.map { it.isChecked },
            animStyleStates = animStyleChecks.map { it.isChecked },
            musicDetailStates = musicChecks.map { it.isChecked },
            musicMoodStates = musicMoodChecks.map { it.isChecked }
        )

        prefs.edit()
            .putString("ui_state", gson.toJson(state))
            .apply()
    }

    private fun restoreUiState() {
        val json = prefs.getString("ui_state", null) ?: return

        try {
            val state = gson.fromJson<UiState>(json, object : TypeToken<UiState>() {}.type)

            basePrompt.setText(state.basePromptText)
            presetNameInput.setText(state.presetNameText)   // ← this is required

            presetSpinner.setSelection(state.selectedPresetIndex.coerceIn(0, presetSpinner.adapter?.count ?: 0))

            genreChecks.forEachIndexed { i, cb -> cb.isChecked = state.genreStates.getOrElse(i) { false } }
            styleChecks.forEachIndexed { i, cb -> cb.isChecked = state.styleStates.getOrElse(i) { false } }
            animStyleChecks.forEachIndexed { i, cb -> cb.isChecked = state.animStyleStates.getOrElse(i) { false } }
            musicChecks.forEachIndexed { i, cb -> cb.isChecked = state.musicDetailStates.getOrElse(i) { false } }
            musicMoodChecks.forEachIndexed { i, cb -> cb.isChecked = state.musicMoodStates.getOrElse(i) { false } }

            generateEnhancedPrompt()
        } catch (e: Exception) {
            Log.e("RestoreUI", "Failed to restore UI state", e)
        }
    }

    private fun clearAllSelections() {
        genreChecks.forEach { it.isChecked = false }
        styleChecks.forEach { it.isChecked = false }
        animStyleChecks.forEach { it.isChecked = false }
        musicChecks.forEach { it.isChecked = false }
        musicMoodChecks.forEach { it.isChecked = false }

        //basePrompt.text.clear()
        //presetNameInput.text.clear()
        result.text = ""

        presetSpinner.setSelection(0)

        prefs.edit().remove("ui_state").apply()

        Toast.makeText(this, "Reset complete", Toast.LENGTH_SHORT).show()
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
                .setMessage("Preset '$name' already exists. Overwrite?")
                .setPositiveButton("Yes") { _, _ -> savePreset(name) }
                .setNegativeButton("No", null)
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
            musicDetails = musicChecks.map { it.isChecked },
            musicMood = musicMoodChecks.map { it.isChecked }
        )

        presets[name] = data
        savePresetsToStorage()
        updatePresetSpinner()
        // presetNameInput.text.clear()   // ← optional: clear after save
        Toast.makeText(this, "Preset '$name' saved", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, "Deleted '$name'", Toast.LENGTH_SHORT).show()
    }

    private fun loadPreset(data: PresetData) {
        genreChecks.forEachIndexed    { i, cb -> cb.isChecked = data.genres.getOrElse(i) { false } }
        styleChecks.forEachIndexed     { i, cb -> cb.isChecked = data.styles.getOrElse(i) { false } }
        animStyleChecks.forEachIndexed { i, cb -> cb.isChecked = data.animStyles.getOrElse(i) { false } }
        musicChecks.forEachIndexed     { i, cb -> cb.isChecked = data.musicDetails.getOrElse(i) { false } }
        musicMoodChecks.forEachIndexed { i, cb -> cb.isChecked = data.musicMood.getOrElse(i) { false } }
    }

    private fun loadPresets() {
        val presetPrefs = getSharedPreferences("presets", Context.MODE_PRIVATE)
        val json = presetPrefs.getString("preset_data", null) ?: return
        val typeToken = object : TypeToken<Map<String, PresetData>>() {}.type
        presets.putAll(gson.fromJson(json, typeToken))
        updatePresetSpinner()
    }

    private fun savePresetsToStorage() {
        val presetPrefs = getSharedPreferences("presets", Context.MODE_PRIVATE)
        presetPrefs.edit().putString("preset_data", gson.toJson(presets)).apply()
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
        val userPrompt = basePrompt.text.toString().trim()

        val enhancements = StringBuilder()
        val musicDetails = StringBuilder()
        val bigEyedPart = StringBuilder()

        val cinematicChecked = findViewById<CheckBox>(R.id.style_cinematic).isChecked
        val animationGenreChecked = findViewById<CheckBox>(R.id.genre_animation).isChecked
        val brightColorsChecked = findViewById<CheckBox>(R.id.anim_bright_colors).isChecked

        val startPhrase = buildString {
            if (cinematicChecked) append("Cinematic ")
            if (animationGenreChecked) append("3D animated ")
            append("movie trailer,")
        }

        if (findViewById<CheckBox>(R.id.style_epic).isChecked) {
            enhancements.append(" Epic cinematic visuals with sweeping camera moves, dramatic lighting, and high-stakes action.")
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

        if (findViewById<CheckBox>(R.id.anim_wholesome).isChecked) {
            enhancements.append(" Wholesome family-friendly tone suitable for all ages.")
        }
        if (findViewById<CheckBox>(R.id.anim_fairytale).isChecked) {
            enhancements.append(" Set in a magical fairy-tale kingdom with wonder and enchantment.")
        }
        if (brightColorsChecked) {
            enhancements.append(" Bright and saturated colors.")
        }
        if (findViewById<CheckBox>(R.id.anim_soft_lighting).isChecked) {
            enhancements.append(" Soft lighting.")
        }
        if (findViewById<CheckBox>(R.id.anim_sweeping_camera).isChecked) {
            enhancements.append(" Sweeping cinematic camera moves and dynamic angles.")
        }
        if (findViewById<CheckBox>(R.id.anim_talking_animal).isChecked) {
            enhancements.append(" Featuring a talking animal.")
        }
        if (findViewById<CheckBox>(R.id.anim_talking_animal_noraccoon).isChecked) {
            enhancements.append(" Featuring a talking animal sidekick (not raccoon).")
        }
        if (findViewById<CheckBox>(R.id.anim_slapstick).isChecked) {
            enhancements.append(" Gentle slapstick humor and lighthearted comedy.")
        }
        if (findViewById<CheckBox>(R.id.anim_emotional_kids).isChecked) {
            enhancements.append(" Emotional moments that resonate with both kids and parents.")
        }

        val selectedMusic = musicChecks.filter { it.isChecked }.map { it.text.toString() }
        if (selectedMusic.isNotEmpty()) {
            musicDetails.append(" featuring ${selectedMusic.joinToString(", ")}")
        }

        val selectedMood = musicMoodChecks.filter { it.isChecked }.map { it.text.toString() }
        if (selectedMood.isNotEmpty()) {
            if (musicDetails.isNotEmpty()) musicDetails.append(", ")
            musicDetails.append("with ${selectedMood.joinToString(" and ")}")
        }

        if (selectedMusic.isEmpty() && selectedMood.isEmpty()) {
            musicDetails.append(" featuring epic orchestral swells, deep braams, and dramatic risers.")
        }

        val musicText = musicDetails.toString().trim()

        if (findViewById<CheckBox>(R.id.style_big_eyed).isChecked) {
            bigEyedPart.append(" music, with big-eyed characters.")
        }

        val enhancedPrompt = buildString {
            append(startPhrase)
            if (enhancements.isNotEmpty()) {
                append(" ")
                append(enhancements.toString().trim())
            }
            if (musicText.isNotEmpty()) {
                append(" ")
                append(musicText)
            }
            if (bigEyedPart.isNotEmpty()) append(bigEyedPart)
            if (userPrompt.isNotEmpty()) {
                append(" ")
                append(userPrompt)
            }
        }.trim()

        result.text = enhancedPrompt
        saveUiState()
    }
}