import lulu from '../assets/avatars/lulu.webp'
import mochi from '../assets/avatars/mochi.webp'
import pico from '../assets/avatars/pico.webp'

/** Poza de profil depinde doar de gen (valorile din backend: FEMALE / MALE / OTHER). */
export const AVATARS = {
  FEMALE: { src: mochi, name: 'Mochi', label: 'Feminin' },
  MALE: { src: pico, name: 'Pico', label: 'Masculin' },
  OTHER: { src: lulu, name: 'Lulu', label: 'Altul' },
}

export const GENDERS = ['FEMALE', 'MALE', 'OTHER']

export const avatarFor = (gender) => AVATARS[gender] ?? AVATARS.OTHER
