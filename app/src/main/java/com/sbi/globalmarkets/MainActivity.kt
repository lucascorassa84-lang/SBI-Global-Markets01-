package com.sbi.globalmarkets

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

class MainActivity : Activity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 64, 40, 32)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        setContentView(ScrollView(this).apply { addView(container) })
        showCurrentScreen()
    }

    private fun text(value: String, size: Float = 18f): TextView = TextView(this).apply {
        text = value
        textSize = size
        setPadding(0, 12, 0, 12)
    }

    private fun button(label: String, action: () -> Unit) {
        container.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
        })
    }

    private fun showCurrentScreen() {
        if (auth.currentUser == null) showLogin() else showHome()
    }

    private fun showLogin() {
        container.removeAllViews()
        container.addView(text("SBI Global Markets", 26f))
        container.addView(text("Acesso seguro"))
        val email = EditText(this).apply {
            hint = "E-mail"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        val password = EditText(this).apply {
            hint = "Senha"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(email)
        container.addView(password)

        fun validate(): Boolean {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text.toString().trim()).matches()) {
                toast("Informe um e-mail válido")
                return false
            }
            if (password.text.length < 6) {
                toast("A senha precisa ter pelo menos 6 caracteres")
                return false
            }
            return true
        }

        button("Entrar") {
            if (validate()) auth.signInWithEmailAndPassword(email.text.toString().trim(), password.text.toString())
                .addOnCompleteListener(this) {
                    if (it.isSuccessful) showHome() else toast("Não foi possível autenticar. Verifique os dados.")
                }
        }
        button("Criar conta") {
            if (validate()) auth.createUserWithEmailAndPassword(email.text.toString().trim(), password.text.toString())
                .addOnCompleteListener(this) {
                    if (it.isSuccessful) showHome() else toast("Não foi possível criar a conta.")
                }
        }
        button("Recuperar senha") {
            val address = email.text.toString().trim()
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()) {
                toast("Informe um e-mail válido")
            } else {
                auth.sendPasswordResetEmail(address).addOnCompleteListener(this) {
                    toast("Se o endereço estiver cadastrado, verifique seu e-mail.")
                }
            }
        }
    }

    private fun showHome() {
        container.removeAllViews()
        container.addView(text("SBI Global Markets", 26f))
        container.addView(text("Sessão autenticada"))
        container.addView(text("Scanner de índices, Forex, metais, petróleo e criptomoedas: integração pendente."))
        container.addView(text("Sinais Price Action M5/M15 e alertas: integração pendente."))
        button("Sair") { auth.signOut(); showLogin() }
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
