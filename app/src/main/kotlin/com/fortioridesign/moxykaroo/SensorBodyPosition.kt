/*
 * Copyright 2026 Fortiori Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fortioridesign.moxykaroo

enum class SensorBodyPosition(val index: Byte, val displayName: String) {
    LEFT_LEG(0, "Left leg"),
    LEFT_CALF(1, "Left calf"),
    LEFT_SHIN(2, "Left shin"),
    LEFT_HAMSTRING(3, "Left hamstring"),
    LEFT_QUAD(4, "Left quad"),
    LEFT_GLUTE(5, "Left glute"),
    RIGHT_LEG(6, "Right leg"),
    RIGHT_CALF(7, "Right calf"),
    RIGHT_SHIN(8, "Right shin"),
    RIGHT_HAMSTRING(9, "Right hamstring"),
    RIGHT_QUAD(10, "Right quad"),
    RIGHT_GLUTE(11, "Right glute"),
    TORSO_BACK(12, "Torso back"),
    LEFT_LOWER_BACK(13, "Left lower back"),
    LEFT_UPPER_BACK(14, "Left upper back"),
    RIGHT_LOWER_BACK(15, "Right lower back"),
    RIGHT_UPPER_BACK(16, "Right upper back"),
    TORSO_FRONT(17, "Torso front"),
    LEFT_ABDOMEN(18, "Left abdomen"),
    LEFT_CHEST(19, "Left chest"),
    RIGHT_ABDOMEN(20, "Right abdomen"),
    RIGHT_CHEST(21, "Right chest"),
    LEFT_ARM(22, "Left arm"),
    LEFT_SHOULDER(23, "Left shoulder"),
    LEFT_BICEP(24, "Left bicep"),
    LEFT_TRICEP(25, "Left tricep"),
    LEFT_BRACHIORADIALIS(26, "Left brachioradialis"),
    LEFT_FOREARM_EXTENSORS(27, "Left forearm extensors"),
    RIGHT_ARM(28, "Right arm"),
    RIGHT_SHOULDER(29, "Right shoulder"),
    RIGHT_BICEP(30, "Right bicep"),
    RIGHT_TRICEP(31, "Right tricep"),
    RIGHT_BRACHIORADIALIS(32, "Right brachioradialis"),
    RIGHT_FOREARM_EXTENSORS(33, "Right forearm extensors"),
    NECK(34, "Neck"),
    THROAT(35, "Throat"),
    WAIST_MID_BACK(36, "Waist mid back"),
    WAIST_FRONT(37, "Waist front"),
    WAIST_LEFT(38, "Waist left"),
    WAIST_RIGHT(39, "Waist right"),
    UNKONWN(-1, "Unknown")
}