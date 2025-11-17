# Draconomics – Gamified Budgeting App

<p align="center">
  <img src="https://github.com/Bexcellent24/Dragonomics/blob/master/Logos/Pages/Login_SignUp%20Page/home_page_logo.png" alt="Draconomics Logo" width="400">
</p>

---

## 1. Overview

**Draconomics** is a gamified budgeting application developed for the **OPSC7311 (Open Source Coding)** module. The app encourages users to build stronger financial habits through interactive gameplay, where managing your budget helps a dragon grow and evolve. Each financial decision influences the dragon's mood and growth, turning financial responsibility into a rewarding and engaging experience.

---

## 2. Purpose

The primary goal of Draconomics is to make personal finance management approachable and motivating. By integrating game mechanics with traditional budgeting features, users are encouraged to maintain consistent saving and spending habits. The project demonstrates the use of open-source tools, proper version control, and collaborative development through GitHub.

---

## 3. Design Considerations

Draconomics was designed with three priorities in mind:

1. **Engagement:** Financial management is often boring or intimidating. The game mechanics (dragon evolution, quests, and achievements) make it fun and goal-driven.
2. **Accessibility:** The UI follows Material Design principles for intuitive navigation and visual consistency.
3. **Scalability:** The app uses the MVVM architecture to separate data, logic, and presentation layers. This makes future development easier to implement.

The app now uses Firebase for cloud based data storage which keeps user records synced across sessions and devices. Multi user functionality supports individual profiles with their own progress and data.

---

## 4. Current Features

- Custom **expense and income nests** for financial organisation
- **Transactions** recorded with "to" and "from" nests for category tracking
- **Dragon customisation** growth, and mood changes based on budgeting performance
- **Quests and achievements** now fully implemented with dynamic rewards
- **History page** showing transactions by selected time periods with built in graphs for visual data analysis
- **Multi-user support** with login and sign-up
- **Goal setting** with minimum and maximum monthly targets
- **Persistent cloud storage** using Firebase
- **Monthly income and expense tracking** supported with built in graphs for visual data analysis

---

## 5. Custom Features

This release introduces a set of new features that expand both the budgeting tools and the game systems.

- **Custom Dragon**
Users have a dragon with full customisation options. The dragon’s mood reflects the user’s budgeting behaviour and it gains experience as expenses are logged. As it levels up it grows through several stages which encourages continued engagement with the app.

- **Nest Editing**
Nests can now be edited at any time. Users can change nest budgets, icons, names, and colours. A nest can also be deleted if it is no longer needed which keeps the budgeting layout clean and relevant.

- **Monthly Reset**
A reset option is available on the nests page. This allows users to clear all nest data at the start of a new budgeting period with one button which makes month to month budgeting easier to manage.

- **Achievements**
The app now includes built in achievements. These award coins for completing tasks such as logging expenses, logging in regularly, and maintaining streaks. This creates reward driven progression that supports consistent financial habits.

---

## 6. Technical Overview

- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **Database:** Firebase
- **UI Framework:** Material Design with ViewBinding
- **Asynchronous Operations:** Kotlin Coroutines and Flow
- **Logging:** Android Logcat used to monitor lifecycle events, data changes, and user actions
- **Version Control:** Managed collaboratively through GitHub

---

## 7. Using GitHub for Version Control

All project source code and documentation are stored in a public GitHub repository. Team members use branches for feature development and merge requests to maintain a clean main branch.

**Repository Link:** [https://github.com/Bexcellent24/Dragonomics](https://github.com/Bexcellent24/Dragonomics)

---

## 8. Continuous Integration (GitHub Actions)

GitHub Actions has been implemented to automatically build and test the app whenever code is pushed to the repository. This ensures that the project compiles correctly and passes all automated tests across different environments.

---

## 9. How to Run

1. Clone the repository:
   ```bash
   git clone https://github.com/Bexcellent24/Dragonomics.git
   ```

2. Open the project in Android Studio (Koala / Jellyfish or newer).

3. Allow Gradle to sync automatically.

4. Connect an Android device or start an emulator.

5. Run the app using **Run → app**.

---

## 10. Demonstration Video

A short presentation video demonstrating the app's key features, gameplay loop, and development process is available on YouTube:

Demo Video With Background Music: https://youtu.be/awkw3RVM6Uw
Demo Video Without Background Music: https://youtu.be/o0KbSR8_hMY

Assets used for the video :
Freepik. (2022). Navy blue geometrical social banner vector | Free Vector. [online] Available at: https://www.freepik.com/free-vector/navy-blue-geometrical-social-banner-vector_36226316.htm#fromView=keyword&page=1&position=7&uuid=cfc661ee-9cdc-4e5e-a8b6-2e0772cac42a&query=Navy+blue+background+geometric+shapes [Accessed 15 Nov. 2025].

lofidreams99 (2025). Lofi background music | Royalty-free Music. [online] Pixabay.com. Available at: https://pixabay.com/music/beats-lofi-background-music-326931/ 
[Accessed 15 Nov. 2025].
---

## 11. Team

- **Rebecca Goodall**
- **Daniel Dennison**
- **Caitlin Jacobs**
- **Njabulo Zikhali**

---

## 12. Submission Includes

- Full Kotlin source code
- Commented codebase
- README file (this document)
- Linked video presentation
- Built .apk file

---

## Licence

This project is developed for educational purposes as part of the OPSC7311 module.

---

**© 2025 Draconomics Team. All rights reserved.**
