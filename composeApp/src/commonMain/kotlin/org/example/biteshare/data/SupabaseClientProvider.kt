package org.example.biteshare.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClientProvider {
    // TODO: move these to secure config (BuildConfig/CI secrets) before shipping.
    private const val SUPABASE_URL = "https://gxpxxzfhfvvtvgvszrhc.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_k7Us_sO5ew4o9pOe_t46jg_QuXXlcU8"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY,
    ) {
        install(Postgrest)
    }
}
