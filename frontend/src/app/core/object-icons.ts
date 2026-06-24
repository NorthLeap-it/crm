import {
  Bell,
  Box,
  Building,
  Calendar,
  CreditCard,
  File,
  FilePen,
  FileText,
  Filter,
  FolderKanban,
  Image as ImageIcon,
  LifeBuoy,
  type LucideIconData,
  MessageCircle,
  Receipt,
  Repeat,
  SquareCheckBig,
  Target,
  TrendingUp,
  User
} from 'lucide-angular';

// Mappa la stringa `ObjectType.icon` (salvata dal backend in kebab-case, sono nomi di icone
// lucide) al componente lucide corrispondente. PER AGGIUNGERE/CAMBIARE un'icona: aggiungi una
// riga qui sotto (chiave = valore esatto del campo `icon` lato backend) e importa il componente
// lucide in cima. Nomi lucide in https://lucide.dev/icons. Se una chiave non e' mappata, ricade
// su Box.
//
// Nota: alcuni nomi lucide sono cambiati tra versioni - es. il backend semina `check-square` ma
// in questa versione di lucide-angular si chiama SquareCheckBig; `file-signature` non esiste piu'
// e usiamo FilePen come sostituto piu' vicino.
const ICONS: Record<string, LucideIconData> = {
  building: Building,
  user: User,
  target: Target,
  filter: Filter,
  'trending-up': TrendingUp,
  'file-text': FileText,
  box: Box,
  'file-signature': FilePen,
  'folder-kanban': FolderKanban,
  'check-square': SquareCheckBig,
  'life-buoy': LifeBuoy,
  file: File,
  image: ImageIcon,
  'message-circle': MessageCircle,
  calendar: Calendar,
  repeat: Repeat,
  receipt: Receipt,
  'credit-card': CreditCard,
  bell: Bell
};

export function resolveObjectIcon(icon: string | null | undefined): LucideIconData {
  return (icon && ICONS[icon]) || Box;
}

// Chiavi icona selezionabili (per i picker, es. scelta icona di un campo/oggetto). Sono le
// stesse chiavi mappate sopra, così tutto si risolve poi con resolveObjectIcon.
export const ICON_KEYS: string[] = Object.keys(ICONS);
